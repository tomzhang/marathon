package mesosphere.marathon.core.matcher.app

import mesosphere.marathon.tasks.TaskQueue

private[core] trait AppOfferMatcherModule {
  def taskQueue: TaskQueue
}
