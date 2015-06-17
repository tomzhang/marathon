package mesosphere.marathon.core.launcher.impl

import java.util.Collections

import mesosphere.marathon.MarathonSchedulerDriverHolder
import mesosphere.marathon.core.launcher.TaskLauncher
import org.apache.mesos.Protos.{ OfferID, TaskInfo }
import org.apache.mesos.SchedulerDriver
import org.slf4j.LoggerFactory

private[impl] class DefaultTaskLauncher(marathonSchedulerDriverHolder: MarathonSchedulerDriverHolder)
    extends TaskLauncher {
  private[this] val log = LoggerFactory.getLogger(getClass)

  override def launchTasks(offerID: OfferID, taskInfos: Seq[TaskInfo]): Unit = {
    withDriver(s"launchTasks(${offerID.getValue}, ${taskInfos.size} tasks)") {
      import scala.collection.JavaConverters._
      _.launchTasks(Collections.singleton(offerID), taskInfos.asJava)
    }
  }

  override def declineOffer(offerID: OfferID): Unit = {
    withDriver(s"declineOffer(${offerID.getValue})") {
      _.declineOffer(offerID)
    }
  }

  private[this] def withDriver(description: => String)(block: SchedulerDriver => Unit) = {
    marathonSchedulerDriverHolder.driver match {
      case Some(driver) => block(driver)
      case None         => log.warn(s"Cannot execute '$description', no driver available")
    }
  }
}
