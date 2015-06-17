package mesosphere.marathon.core.matcher.util

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import mesosphere.marathon.core.base.Clock
import mesosphere.marathon.core.matcher.OfferMatcher
import mesosphere.marathon.core.matcher.OfferMatcher.MatchedTasks
import mesosphere.marathon.state.Timestamp
import org.apache.mesos.Protos.Offer

import scala.concurrent.Future

/**
  * Provides a thin wrapper around an OfferMatcher implemented as an actors.
  */
class ActorOfferMatcher(clock: Clock, actorRef: ActorRef) extends OfferMatcher {
  def processOffer(deadline: Timestamp, offer: Offer): Future[MatchedTasks] = {
    implicit val timeout: Timeout = clock.now().until(deadline)
    val answerFuture = actorRef ? ActorOfferMatcher.MatchOffer(deadline, offer)
    answerFuture.mapTo[MatchedTasks]
  }
}

object ActorOfferMatcher {
  /**
    * Send to an offer matcher to request a match.
    *
    * This should always be replied to with a LaunchTasks message.
    */
  case class MatchOffer(matchingDeadline: Timestamp, remainingOffer: Offer)
}
