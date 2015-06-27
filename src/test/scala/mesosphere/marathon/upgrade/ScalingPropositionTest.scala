package mesosphere.marathon.upgrade

import mesosphere.marathon.Protos.MarathonTask
import org.scalatest.{ FunSuite, Matchers }

class ScalingPropositionTest extends FunSuite with Matchers {

  test("custom apply - tasksToKill: empty should be None") {
    val proposition = new ScalingProposition(Seq.empty[MarathonTask], 0)
    proposition.tasksToKill shouldBe empty
  }

  test("custom apply - tasksToKill: nonEmpty should be Some") {
    val task = MarathonTask.getDefaultInstance
    val proposition = new ScalingProposition(Seq(task), 0)
    proposition.tasksToKill shouldEqual Some(Seq(task))
  }

  test("custom apply - tasksToStart: 0 should be None") {
    val proposition = new ScalingProposition(Seq.empty[MarathonTask], 0)
    proposition.tasksToStart shouldBe empty
  }

  test("custom apply - tasksToStart: negative number should be None") {
    val proposition = new ScalingProposition(Seq.empty[MarathonTask], -42)
    proposition.tasksToStart shouldBe empty
  }

  test("custom apply - tasksToStart: positive number should be Some") {
    val proposition = new ScalingProposition(Seq.empty[MarathonTask], 42)
    proposition.tasksToStart shouldBe Some(42)
  }

}
