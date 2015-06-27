package mesosphere.marathon.upgrade

import mesosphere.marathon.Protos.MarathonTask

object ScalingCalculator {

  def scalingProposition(runningTasks: Set[MarathonTask],
                         toKill: Option[Set[MarathonTask]],
                         scaleTo: Int): ScalingProposition = {
    val (sentencedAndRunning, notSentencedAndRunning) = runningTasks partition toKill.getOrElse(Set.empty)
    val killCount = math.max(runningTasks.size - scaleTo, sentencedAndRunning.size)
    val ordered = sentencedAndRunning.toSeq ++ notSentencedAndRunning.toSeq.sortBy(_.getStartedAt).reverse
    val tasksToKill = ordered.take(killCount)
    val tasksToStart = scaleTo - runningTasks.size + killCount

    new ScalingProposition(tasksToKill, tasksToStart)
  }

}
