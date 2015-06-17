package mesosphere.marathon.core.matcher.app.impl

import akka.actor.{Actor, Props}
import mesosphere.marathon.state.{PathId, AppDefinition}
import mesosphere.marathon.tasks.TaskQueue.QueuedTaskCount

object CoreTaskQueueActor {
  def props(appActorProps: (AppDefinition, Int) => Props): Props = {
    Props(new CoreTaskQueueActor(appActorProps))
  }

  // messages to the core task actor which correspond directly to the TaskQueue interface
  sealed trait Request
  case object List extends Request
  case class Count(appId: PathId) extends Request
  case object ListApps extends Request
  case object ListAppsWithDelay extends Request
  case class Purge(appId: PathId) extends Request
  case class Add(app: AppDefinition, count: Int) extends Request
}

private[impl] class CoreTaskQueueActor(appActorProps: (AppDefinition, Int) => Props) extends Actor {
  import CoreTaskQueueActor._



  override def receive: Receive = {
    case List =>
    case _ => // TODO: implement
  }
}
