package mesosphere.marathon.core.base.actors

import akka.actor.ActorSystem
import mesosphere.marathon.core.base.ShutdownHookModule
import org.slf4j.LoggerFactory
import scala.concurrent.duration._

/**
  * Contains basic dependencies used throughout the application disregarding the concrete function.
  */
trait ActorsModule {
  def actorSystem: ActorSystem
}

class DefaultActorsModule(shutdownHookModule: ShutdownHookModule) extends ActorsModule {
  private[this] val log = LoggerFactory.getLogger(getClass)

  override val actorSystem: ActorSystem = ActorSystem()

  shutdownHookModule.onShutdown {
    log.info("Shutting down actor system")
    actorSystem.shutdown()
    actorSystem.awaitTermination(10.seconds)
  }
}
