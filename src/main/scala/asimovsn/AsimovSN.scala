package asimovsn

import java.nio.file.Path

object AsimovSN:
  def command(p: Path) =
    s"tmutil addexclusion ${p.toAbsolutePath().toString()}"

  def main(args: Array[String]): Unit =
    Config.parse(args) match
      case None => println("failed to parse")
      case Some(config) =>
        println("starting")
        def matches(p: os.Path) =
          config.rules.find(rule => p.toNIO.endsWith(rule.sentinel))

        val files = os.walk.stream(config.basePath)

        val occurrences = files
          .map(p => matches(p).map(r => (p, r)))
          .filter(_.nonEmpty)
          .map(_.get)

        val exclusions = occurrences.map((sentinelPath, tup) =>
          sentinelPath.toNIO.getParent.resolve(tup._1)
        )

        val commands = exclusions.map(command)

        commands.foreach { cmdString => 
          if config.dryRun then println(cmdString)
          else os.call(cmdString)
        }
        println("done")
