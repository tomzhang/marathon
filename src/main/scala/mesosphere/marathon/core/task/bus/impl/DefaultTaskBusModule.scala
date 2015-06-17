package mesosphere.marathon.core.task.bus.impl

import mesosphere.marathon.core.task.bus.{ TaskStatusObservable, TaskStatusEmitter, TaskBusModule }

private[core] class DefaultTaskBusModule extends TaskBusModule {
  override lazy val taskStatusEmitter: TaskStatusEmitter =
    new DefaultTaskStatusEmitter(internalTaskStatusEventStream)
  override lazy val taskStatusObservable: TaskStatusObservable =
    new DefaultTaskStatusObservable(internalTaskStatusEventStream)

  private[this] lazy val internalTaskStatusEventStream = new InternalTaskStatusEventStream()
}
