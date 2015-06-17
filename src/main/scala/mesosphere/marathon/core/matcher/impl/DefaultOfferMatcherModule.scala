package mesosphere.marathon.core.matcher.impl

import akka.actor.ActorRef
import mesosphere.marathon.core.base.actors.ActorsModule
import mesosphere.marathon.core.base.{ ClockModule, RandomModule }
import mesosphere.marathon.core.matcher.util.ActorOfferMatcher
import mesosphere.marathon.core.matcher.{ OfferMatcher, OfferMatcherManager, OfferMatcherModule }
import mesosphere.marathon.core.task.bus.TaskBusModule

import scala.concurrent.duration._

private[core] class DefaultOfferMatcherModule(
  taskModule: TaskBusModule,
  clockModule: ClockModule,
  randomModule: RandomModule,
  actorsModule: ActorsModule)
    extends OfferMatcherModule {

  private[this] lazy val offerMatcherMultiplexer: ActorRef = {
    val props = OfferMatcherMultiplexerActor.props(
      randomModule.random,
      clockModule.clock,
      taskModule.taskStatusEmitter)
    val actorRef = actorsModule.actorSystem.actorOf(props, "OfferMatcherMultiplexer")
    implicit val dispatcher = actorsModule.actorSystem.dispatcher
    actorsModule.actorSystem.scheduler.schedule(
      0.seconds, DefaultOfferMatcherModule.launchTokenInterval, actorRef,
      OfferMatcherMultiplexerActor.SetTaskLaunchTokens(DefaultOfferMatcherModule.launchTokensPerInterval))
    actorRef
  }

  override val offerMatcher: OfferMatcher = new ActorOfferMatcher(clockModule.clock, offerMatcherMultiplexer)

  override val subOfferMatcherManager: OfferMatcherManager = new ActorOfferMatcherManager(offerMatcherMultiplexer)
}

private[core] object DefaultOfferMatcherModule {
  val launchTokenInterval: FiniteDuration = 30.seconds
  val launchTokensPerInterval: Int = 1000
}
