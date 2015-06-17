package mesosphere.marathon.core.matcher

trait OfferMatcherModule {
  def offerMatcher: OfferMatcher
  def subOfferMatcherManager: OfferMatcherManager
}

