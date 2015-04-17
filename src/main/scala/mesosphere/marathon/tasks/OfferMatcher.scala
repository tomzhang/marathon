package mesosphere.marathon.tasks

import java.util

import com.google.inject.Inject
import mesosphere.marathon.Protos.MarathonTask
import mesosphere.marathon.ZookeeperConf
import mesosphere.marathon.state.AppDefinition
import org.apache.mesos.Protos._
import org.apache.mesos.SchedulerDriver
import org.rogach.scallop.ScallopConf
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq

trait OfferMatcher {
  /**
    * Process the given offers. All offers should either be used for launching tasks or declined
    * via the given driver.
    */
  def processResourceOffers(driver: SchedulerDriver, offersList: Iterable[Offer]): Unit
}

trait IterativeOfferMatcherConfig extends ScallopConf {
  lazy val maxTasksPerOffer = opt[Int]("max_tasks_per_offer",
    descr = "Maximally launch this number of tasks per offer.",
    default = Some(1),
    noshort = true)
}

class IterativeOfferMatcher @Inject() (
  iterativeOfferMatcherConfig: IterativeOfferMatcherConfig,
  taskQueue: TaskQueue,
  taskTracker: TaskTracker,
  taskFactory: TaskFactory)
    extends OfferMatcher {

  private[this] val log = LoggerFactory.getLogger(getClass)

  private[this] val maxTasksPerOffer: Int = iterativeOfferMatcherConfig.maxTasksPerOffer.get.getOrElse(1)
  require(maxTasksPerOffer > 0, "We need to be allowed to launch at least one task per offer")

  /**
    * @param hot [[OfferUsage]]s which can still potentially satisfy scheduled task resources
    * @param depleted [[OfferUsages]]s which we already failed to match in the last cycle
    */
  case class OfferUsages(hot: Vector[OfferUsage] = Vector.empty, depleted: Vector[OfferUsage] = Vector.empty) {
    def addDepleted(offer: OfferUsage): OfferUsages = copy(depleted = depleted :+ offer)
    def addHot(offer: OfferUsage): OfferUsages = copy(hot = hot :+ offer)

    def usages: Vector[OfferUsage] = hot ++ depleted
  }
  case class OfferUsage(remainingOffer: Offer, scheduledTasks: Vector[TaskInfo] = Vector.empty) {
    def addTask(taskInfo: TaskInfo): OfferUsage = {
      copy(
        scheduledTasks = scheduledTasks :+ taskInfo,
        remainingOffer = deductTask(remainingOffer, taskInfo)
      )
    }

    /** Deducts the resources used by the task from the given offer. */
    private[this] def deductTask(offer: Offer, taskInfo: TaskInfo): Offer = {
      val newResources = ResourceUtil.consumeResources(
        offer.getResourcesList.asScala, taskInfo.getResourcesList.asScala)

      log.debug("old resources {}", offer.getResourcesList)
      log.debug("used resources {}", taskInfo.getResourcesList)
      log.debug("offer left {}", newResources)

      offer.toBuilder.clearResources().addAllResources(newResources.asJava).build()
    }
  }

  def processResourceOffers(driver: SchedulerDriver, offersList: Iterable[Offer]): Unit = {
    log.info("started processing {} offers", offersList.size)

    def matchOffers(offers: OfferUsages): OfferUsages = {
      log.debug(s"processing ${offers.hot.size} hot offers")
      offers.hot.foldLeft(OfferUsages(depleted = offers.depleted)) {
        case (processed, hot) =>
          val offer = hot.remainingOffer

          try {
            taskQueue.pollMatching { app =>
              taskFactory.newTask(app, offer).map(app -> _)
            } match {
              case Some((app: AppDefinition, (taskInfo: TaskInfo, marathonTask: MarathonTask))) =>

                log.debug("Adding task for launching: " + taskInfo)
                taskTracker.created(app.id, marathonTask)
                processed.addHot(hot.addTask(taskInfo))

              // here it is assumed that the health checks for the current
              // version are already running.
              case None =>
                processed.addDepleted(hot)
            }
          }
          catch {
            case t: Throwable =>
              log.error("Caught an exception. Declining offer.", t)
              processed.addDepleted(hot)
          }
      }
    }

    val offerIterator = new Iterator[OfferUsages] {
      private[this] var offers = OfferUsages(hot = offersList.map(OfferUsage(_)).toVector)
      override def hasNext: Boolean = offers.hot.nonEmpty
      override def next(): OfferUsages = {
        offers = matchOffers(offers)
        offers
      }
    }

    val finalOfferUsage = offerIterator.toStream.take(maxTasksPerOffer).last

    var usedOffers = 0
    var launchedTasks = 0
    var declinedOffers = 0
    for (offerUsage <- finalOfferUsage.usages) {
      val offer = offerUsage.remainingOffer
      if (offerUsage.scheduledTasks.nonEmpty) {
        val offerIds: util.Collection[OfferID] = Seq(offer.getId).asJavaCollection
        val tasks: util.Collection[TaskInfo] = offerUsage.scheduledTasks.asJavaCollection
        if (log.isDebugEnabled) {
          log.debug("for offer {} launch tasks {}", Seq(offer.getId, tasks.asScala.map(_.getTaskId)): _*)
        }
        driver.launchTasks(offerIds, tasks)
        usedOffers += 1
        launchedTasks += tasks.size()
      }
      else {
        log.debug("declining offer", offer.getId)
        driver.declineOffer(offer.getId)
        declinedOffers += 1
      }
    }

    log.info(s"Launched $launchedTasks tasks on $usedOffers offers, declining $declinedOffers")
  }
}
