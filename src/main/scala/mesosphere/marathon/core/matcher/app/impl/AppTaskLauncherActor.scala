package mesosphere.marathon.core.matcher.app.impl

import akka.actor.{Actor, Props}
import akka.event.LoggingReceive
import mesosphere.marathon.Protos.MarathonTask
import mesosphere.marathon.core.base.Clock
import mesosphere.marathon.core.matcher.OfferMatcher.MatchedTasks
import mesosphere.marathon.core.matcher.{OfferMatcher, OfferMatcherManager}
import mesosphere.marathon.core.matcher.util.ActorOfferMatcher
import mesosphere.marathon.core.task.bus.TaskStatusObservable.TaskStatusUpdate
import mesosphere.marathon.core.task.bus.{MarathonTaskStatus, TaskStatusObservable}
import mesosphere.marathon.state.AppDefinition
import mesosphere.marathon.tasks.TaskFactory.CreatedTask
import mesosphere.marathon.tasks.{TaskFactory, TaskTracker}
import rx.lang.scala.Subscription

private[impl] object AppTaskLauncherActor {
  def props(
    offerMatcherManager: OfferMatcherManager,
                                              clock: Clock,
                                              taskFactory: TaskFactory,
                                              taskStatusObservable: TaskStatusObservable,
                                              taskTracker: TaskTracker)(
                                              app: AppDefinition,
                                              initialCount: Int): Props = {
    Props(new AppTaskLauncherActor(
      offerMatcherManager,
      clock, taskFactory, taskStatusObservable, taskTracker, app, initialCount))
  }

  sealed trait Requests

  /**
    * Increase the task count of the receiver.
    * The actor responds with a [[Count]] message.
    */
  case class AddTasks(count: Int) extends Requests
  /**
    * Get the current count.
    * The actor responds with a [[Count]] message.
    */
  case object GetCount extends Requests

  sealed trait Response
  case class Count(app: AppDefinition, count: Int)
}

/**
  * Allows processing offers for starting tasks for the given app.
  */
private class AppTaskLauncherActor(
  offerMatcherManager: OfferMatcherManager,
  clock: Clock,
  taskFactory: TaskFactory,
  taskStatusObservable: TaskStatusObservable,
  taskTracker: TaskTracker,
  app: AppDefinition,
  initialCount: Int)
    extends Actor {

  var count = initialCount
  var taskStatusUpdateSubscription: Subscription = _

  var runningTasks: Set[MarathonTask] = _
  var runningTasksMap: Map[String, MarathonTask] = _

  var myselfAsOfferMatcher: OfferMatcher = _

  override def preStart(): Unit = {
    super.preStart()

    myselfAsOfferMatcher = new ActorOfferMatcher(clock, self)
    offerMatcherManager.addOfferMatcher(myselfAsOfferMatcher)(context.dispatcher)
    taskStatusUpdateSubscription = taskStatusObservable.forAppId(app.id).subscribe(self ! _)
    runningTasks = taskTracker.get(app.id)
    runningTasksMap = runningTasks.map(task => task.getId -> task).toMap
  }

  override def postStop(): Unit = {
    taskStatusUpdateSubscription.unsubscribe()
    offerMatcherManager.removeOfferMatcher(myselfAsOfferMatcher)(context.dispatcher)

    super.postStop()
  }

  override def receive: Receive = {
    Seq(
      receiveTaskStatusUpdate,
      receiveGetCurrentCount,
      receiveAddCount,
      receiveProcessOffers
    ).reduce(_.orElse[Any, Unit](_))
  }

  private[this] def receiveTaskStatusUpdate: Receive = LoggingReceive.withLabel("receiveTaskStatusUpdate") {
    case TaskStatusUpdate(_, taskId, MarathonTaskStatus.Terminal(_)) =>
      runningTasksMap.get(taskId.getValue).foreach { marathonTask =>
        runningTasksMap -= taskId.getValue
        runningTasks -= marathonTask
      }
  }

  private[this] def receiveGetCurrentCount: Receive = LoggingReceive.withLabel("receiveGetCurrentCount") {
    case AppTaskLauncherActor.GetCount => sender() ! AppTaskLauncherActor.Count(app, count)
  }

  private[this] def receiveAddCount: Receive = LoggingReceive.withLabel("receiveAddCount") {
    case AppTaskLauncherActor.AddTasks(addCount) =>
      count += addCount
      sender() ! AppTaskLauncherActor.Count(app, count)
  }

  private[this] def receiveProcessOffers: Receive = LoggingReceive.withLabel("receiveProcessOffers") {
    case ActorOfferMatcher.MatchOffer(deadline, offer) if clock.now() > deadline =>
      sender ! MatchedTasks(offer.getId, Seq.empty)

    case ActorOfferMatcher.MatchOffer(deadline, offer) =>
      val tasks = taskFactory.newTask(app, offer, runningTasks) match {
        case Some(CreatedTask(mesosTask, marathonTask)) =>
          taskTracker.created(app.id, marathonTask)
          runningTasks += marathonTask
          runningTasksMap += marathonTask.getId -> marathonTask
          count -= 1
          if (count <= 0) {
            // do not stop myself to prevent race condition
            context.parent ! CoreTaskQueueActor.Purge(app.id)
          }
          Seq(mesosTask)
        case None => Seq.empty
      }
      sender ! MatchedTasks(offer.getId, tasks)
  }
}
