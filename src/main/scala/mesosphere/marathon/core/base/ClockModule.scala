package mesosphere.marathon.core.base

trait ClockModule {
  def clock: Clock
}

class DefaultClockModule extends ClockModule {
  override val clock: Clock = new DefaultClock
}
