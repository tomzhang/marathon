package mesosphere.marathon.upgrade

import mesosphere.marathon.Protos.MarathonTask

class ScalingProposition(private val candidatesToKill: Seq[MarathonTask],
                         private val numberOfTasksToStart: Int) {
  val tasksToKill = if (candidatesToKill.nonEmpty) Some(candidatesToKill) else None
  val tasksToStart = if (numberOfTasksToStart > 0) Some(numberOfTasksToStart) else None
}
