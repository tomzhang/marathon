package mesosphere.marathon.core.task.bus.impl

import mesosphere.marathon.core.task.bus.TaskStatusObservable
import mesosphere.marathon.core.task.bus.TaskStatusObservable.TaskStatusUpdate
import mesosphere.marathon.state.PathId
import rx.lang.scala.{ Observable, Subscription }

private[bus] class DefaultTaskStatusObservable(eventStream: InternalTaskStatusEventStream)
    extends TaskStatusObservable {

  override def forAll: Observable[TaskStatusUpdate] = forAppId(PathId.empty)

  override def forAppId(appId: PathId): Observable[TaskStatusUpdate] = {
    Observable.create { observer =>

      eventStream.subscribe(observer, appId)

      new Subscription {
        override def unsubscribe(): Unit = {
          eventStream.unsubscribe(observer, appId)
          super.unsubscribe()
        }
      }
    }
  }
}

