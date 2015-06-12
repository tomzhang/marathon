package mesosphere.marathon.upgrade

import mesosphere.marathon.Protos.MarathonTask
import mesosphere.marathon.state.Timestamp
import mesosphere.marathon.tasks.MarathonTasks
import org.scalatest.FunSuite

class DeploymentDeathRowTest extends FunSuite {

  private def createTask(index: Long) = MarathonTasks.makeTask(s"task-$index", "", Nil, Nil, version = Timestamp(index))

  test("Determine tasks to kill when there are no sentenced") {
    val runningTasks = Set.empty[MarathonTask]
    val sentencedToDeath = Set.empty[MarathonTask]
    val scaleTo = 5
    val deathRow = DeploymentDeathRow(runningTasks, sentencedToDeath, scaleTo).tasksToBeKilled
    assert(deathRow.isEmpty, "No tasks should be killed")
  }

  test("Determine tasks to kill when none are sentenced and no need for scaling") {
    val runningTasks: Set[MarathonTask] = Set(
      createTask(1),
      createTask(2),
      createTask(3)
    )
    val sentencedToDeath = Set.empty[MarathonTask]
    val scaleTo = 5
    val deathRow = DeploymentDeathRow(runningTasks, sentencedToDeath, scaleTo).tasksToBeKilled
    assert(deathRow.isEmpty, "No tasks should be killed")
  }

  test("Determine tasks to kill when scaling to 0") {
    val runningTasks: Set[MarathonTask] = Set(
      createTask(1),
      createTask(2),
      createTask(3)
    )
    val sentencedToDeath = Set.empty[MarathonTask]
    val scaleTo = 0
    val deathRow = DeploymentDeathRow(runningTasks, sentencedToDeath, scaleTo).tasksToBeKilled
    assert(deathRow.size == runningTasks.size, "All tasks should be killed")
  }

  test("Determine tasks to kill w/ invalid task") {
    val task_1 = createTask(1)
    val task_2 = createTask(2)
    val task_3 = createTask(3)

    val runningTasks: Set[MarathonTask] = Set(task_1, task_2, task_3)
    val sentencedToDeath = Set(task_2, task_3, MarathonTasks.makeTask("alreadyKilled", "", Nil, Nil, version = Timestamp(0L)))
    val scaleTo = 3
    val deathRow = DeploymentDeathRow(runningTasks, sentencedToDeath, scaleTo).tasksToBeKilled
    assert(deathRow.size == 2, "2 tasks should be killed")
    assert(deathRow == Seq(task_2, task_3), "tasks 2 and 3 should be killed")
  }

  test("Determine tasks to kill w/ invalid task 2") {
    val task_1 = createTask(1)
    val task_2 = createTask(2)
    val task_3 = createTask(3)
    val task_4 = createTask(4)

    val runningTasks: Set[MarathonTask] = Set(task_1, task_2, task_3, task_4)
    val sentencedToDeath = Set(createTask(42))
    val scaleTo = 3
    val deathRow = DeploymentDeathRow(runningTasks, sentencedToDeath, scaleTo).tasksToBeKilled
    assert(deathRow.size == 1, "One task should be killed")
    assert(deathRow == Seq(task_1), "tasks 1 should be killed")
  }

}
