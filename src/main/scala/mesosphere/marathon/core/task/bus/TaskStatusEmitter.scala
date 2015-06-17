package mesosphere.marathon.core.task.bus

trait TaskStatusEmitter {
  def publish(status: TaskStatusObservable.TaskStatusUpdate)
}
