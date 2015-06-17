package mesosphere.marathon.core.launcher.impl

import mesosphere.marathon.MarathonSchedulerDriverHolder
import mesosphere.marathon.core.launcher.{ OfferProcessor, TaskLauncher, LauncherModule }
import mesosphere.marathon.core.matcher.OfferMatcherModule

private[core] class DefaultLauncherModule(
  marathonSchedulerDriverHolder: MarathonSchedulerDriverHolder,
  offerMatcherModule: OfferMatcherModule)
    extends LauncherModule {

  override lazy val offerProcessor: OfferProcessor =
    new DefaultOfferProcessor(offerMatcherModule.offerMatcher, taskLauncher)

  override lazy val taskLauncher: TaskLauncher = new DefaultTaskLauncher(marathonSchedulerDriverHolder)
}
