package mesosphere.marathon.tasks

import mesosphere.marathon.state.{ AppDefinition, PathId }

import scala.collection.immutable.Seq
import scala.concurrent.duration.Deadline

object TaskQueue {
  protected[marathon] case class QueuedTaskCount(app: AppDefinition, count: Int)
}

/**
  * Utility class to stage tasks before they get scheduled
  */
trait TaskQueue {

  import mesosphere.marathon.tasks.TaskQueue._

  def list: Seq[QueuedTaskCount]
  def listWithDelay: Seq[(QueuedTaskCount, Deadline)]

  def listApps: Seq[AppDefinition]

  def add(app: AppDefinition, count: Int = 1): Unit
  def count(appId: PathId): Int
  def purge(appId: PathId): Unit
}
