package mesosphere.marathon.core.matcher.app.impl

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import mesosphere.marathon.state.{AppDefinition, PathId}
import mesosphere.marathon.tasks.TaskQueue
import mesosphere.marathon.tasks.TaskQueue.QueuedTaskCount

import scala.collection.immutable.Seq
import scala.concurrent.Await
import scala.concurrent.duration.{Deadline, _}
import scala.reflect.ClassTag

private[impl] class CoreTaskQueue(actorRef: ActorRef) extends TaskQueue {
  override def list: Seq[QueuedTaskCount] = askActor(CoreTaskQueueActor.List)
  override def count(appId: PathId): Int = askActor(CoreTaskQueueActor.Count(appId))
  override def listApps: Seq[AppDefinition] = askActor(CoreTaskQueueActor.ListApps)
  override def listWithDelay: Seq[(QueuedTaskCount, Deadline)] = askActor(CoreTaskQueueActor.ListAppsWithDelay)
  override def purge(appId: PathId): Unit = askActor(CoreTaskQueueActor.Purge(appId))
  override def add(app: AppDefinition, count: Int): Unit = askActor(CoreTaskQueueActor.Add(app, count))

  private[this] def askActor[R: ClassTag, T](message: T): R = {
    implicit val timeout: Timeout = 1.second
    val answerFuture = actorRef ? message
    Await.result(answerFuture.mapTo[R], 1.second)
  }
}
