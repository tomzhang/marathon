package mesosphere.marathon.core.base

import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

trait ShutdownHookModule {
  def onShutdown(block: => Unit): Unit

  def shutdown(): Unit
}

class DefaultShutdownHookModule extends ShutdownHookModule {
  private[this] val log = LoggerFactory.getLogger(getClass)
  private[this] var shutdownHooks = List.empty[() => Unit]

  override def onShutdown(block: => Unit): Unit = {
    shutdownHooks +:= { () => block }
  }

  override def shutdown(): Unit = {
    shutdownHooks.foreach { hook =>
      try hook()
      catch {
        case NonFatal(e) => log.error("while executing shutdown hook", e)
      }
    }
  }
}
