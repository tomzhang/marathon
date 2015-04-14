package mesosphere.marathon

import org.rogach.scallop.ScallopConf

trait LogConf extends ScallopConf {
  lazy val logQuorum = opt[Int]("log_quorum",
    descr = "Quorum for replicated log state",
    noshort = true
  )

  lazy val logPath = opt[String]("log_path",
    descr = "Path to store replicated log",
    noshort = true
  )

  lazy val logDiffsBetweenSnapshots = opt[Int]("log_diffs_between_snapshots",
    descr = "Number of diffs between snapshots in replicated log",
    noshort = true
  )
}
