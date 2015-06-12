package mesosphere.marathon.upgrade

import mesosphere.marathon.Protos.MarathonTask

private[upgrade] case class DeploymentDeathRow(runningTasks: Set[MarathonTask],
                                               sentencedToDeath: Set[MarathonTask],
                                               scaleTo: Int) {

  val tasksToBeKilled: Seq[MarathonTask] = {
    val (sentencedAndRunning, notSentencedAndRunning) = runningTasks partition sentencedToDeath
    val keepAliveCount = runningTasks.size - math.max(sentencedAndRunning.size, runningTasks.size - scaleTo)
    // is getStartedAt a good value to sortBy?
    val ordered = sentencedAndRunning.toSeq ++ notSentencedAndRunning.toSeq.sortBy(_.getStartedAt)
    val result = ordered.dropRight(keepAliveCount)
    result
  }

}