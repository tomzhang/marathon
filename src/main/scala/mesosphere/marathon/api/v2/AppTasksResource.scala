package mesosphere.marathon.api.v2

import javax.inject.Inject
import javax.ws.rs._
import javax.ws.rs.core.{ MediaType, Response }

import com.codahale.metrics.annotation.Timed
import org.apache.log4j.Logger

import mesosphere.marathon.api.v2.json.EnrichedTask
import mesosphere.marathon.api.{ EndpointsHelper, RestResource }
import mesosphere.marathon.health.HealthCheckManager
import mesosphere.marathon.state.PathId._
import mesosphere.marathon.state.{ AppDefinition, Timestamp, GroupManager, PathId }
import mesosphere.marathon.tasks.TaskTracker
import mesosphere.marathon.{ MarathonConf, MarathonSchedulerService }
import mesosphere.marathon.Protos.MarathonTask

@Produces(Array(MediaType.APPLICATION_JSON))
@Consumes(Array(MediaType.APPLICATION_JSON))
class AppTasksResource @Inject() (service: MarathonSchedulerService,
                                  taskTracker: TaskTracker,
                                  healthCheckManager: HealthCheckManager,
                                  val config: MarathonConf,
                                  groupManager: GroupManager) extends RestResource {

  val log = Logger.getLogger(getClass.getName)
  val GroupTasks = """^((?:.+/)|)\*$""".r

  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Timed
  def indexJson(@PathParam("appId") appId: String): Response = {

    def tasks(appIds: Set[PathId]): Set[EnrichedTask] = for {
      id <- appIds
      health = result(healthCheckManager.statuses(id))
      task <- taskTracker.get(id)
    } yield EnrichedTask(id, task, health.getOrElse(task.getId, Nil))

    val matchingApps = appId match {
      case GroupTasks(gid) =>
        result(groupManager.group(gid.toRootPath))
          .map(_.transitiveApps.map(_.id))
          .getOrElse(Set.empty)
      case _ => Set(appId.toRootPath)
    }

    val running = matchingApps.filter(taskTracker.contains)

    if (running.isEmpty) unknownApp(appId.toRootPath) else ok(Map("tasks" -> tasks(running)))
  }

  @GET
  @Produces(Array(MediaType.TEXT_PLAIN))
  @Timed
  def indexTxt(@PathParam("appId") appId: String): Response = {
    val id = appId.toRootPath
    service.getApp(id).fold(unknownApp(id)) { app =>
      ok(EndpointsHelper.appsToEndpointString(taskTracker, Seq(app), "\t"))
    }
  }

  @DELETE
  @Timed
  def deleteMany(@PathParam("appId") appId: String,
                 @QueryParam("host") host: String,
                 @QueryParam("scale") scale: Boolean = false): Response = {
    val pathId = appId.toRootPath
    def findToKill(appTasks: Set[MarathonTask]): Set[MarathonTask] = Option(host).fold(appTasks) { hostname =>
      appTasks.filter(_.getHost == hostname || hostname == "*")
    }

    killTasks(pathId, findToKill, scale)
  }

  @DELETE
  @Path("{taskId}")
  @Timed
  def deleteOne(@PathParam("appId") appId: String,
                @PathParam("taskId") id: String,
                @QueryParam("scale") scale: Boolean = false): Response = {
    val pathId = appId.toRootPath
    def findToKill(appTasks: Set[MarathonTask]): Set[MarathonTask] = appTasks.find(_.getId == id).toSet

    killTasks(pathId, findToKill, scale)
  }

  private def killTasks(pathId: PathId,
                        findToKill: (Set[MarathonTask] => Set[MarathonTask]),
                        scale: Boolean): Response = {
    import mesosphere.util.ThreadPoolContext.context

    if (taskTracker.contains(pathId)) {
      val tasks = taskTracker.get(pathId)
      val toKill = findToKill(tasks)
      if (scale) {
        val future = groupManager.group(pathId.parent).map {
          case Some(group) =>
            def updateAppFunc(current: AppDefinition) = current.copy(instances = current.instances - toKill.size)
            val deploymentPlan = result(groupManager.updateApp(
              pathId, updateAppFunc, Timestamp.now(), force = false, toKill = toKill))
            deploymentResult(deploymentPlan)

          case None => unknownGroup(pathId.parent)
        }
        result(future)
      }
      else {
        // setting scale = false in order to make the differing behaviour explicit
        service.killTasks(pathId, toKill, scale = false)
      }

      if (toKill.size == 1) {
        ok(Map("task" -> toKill.head))
      }
      else {
        ok(Map("tasks" -> toKill))
      }
    }
    else {
      unknownApp(pathId)
    }
  }

}
