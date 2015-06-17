package mesosphere.marathon.core.task.bus

import mesosphere.marathon.core.task.bus.TaskStatusObservable.TaskStatusUpdate
import mesosphere.marathon.state.{ PathId, Timestamp }
import mesosphere.marathon.tasks.TaskIdUtil
import org.apache.mesos.Protos.TaskID
import rx.lang.scala.Observable

object TaskStatusObservable {
  case class TaskStatusUpdate(timestamp: Timestamp, taskId: TaskID, status: MarathonTaskStatus) {
    lazy val appId: PathId = TaskIdUtil.appId(taskId)
  }
}

/**
  * Allows subscription to TaskStatus updates.
  */
trait TaskStatusObservable {
  def forAll: Observable[TaskStatusUpdate]
  def forAppId(appId: PathId): Observable[TaskStatusUpdate]
}

