package mesosphere.marathon.core.base.leadership

import mesosphere.marathon.api.LeaderInfo

class DefaultLeadershipModule(val leaderInfo: LeaderInfo) extends LeadershipModule
