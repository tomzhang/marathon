package mesosphere.marathon.core.base.leadership

import mesosphere.marathon.api.LeaderInfo

trait LeadershipModule {
  def leaderInfo: LeaderInfo
}
