package asimovsn

import java.nio.file.Path

object AsimovSN:
  def command(p: Path) =
    s"tmutil addexclusion ${p.toAbsolutePath().toString()}"

  def main(args: Array[String]): Unit =
    Config.parse(args) match
      case None         => println("failed to parse")
      case Some(config) =>
        println("starting")
        val files = os.walk.stream(config.basePath).map(_.toNIO)

        val occurrences = files
          .map(p => config.rules.find(_.matches(p)).map(r => (p, r)))
          .filter(_.nonEmpty)
          .map(_.get)

        val exclusions = occurrences.map((sentinelPath, tup) =>
          sentinelPath.getParent.resolve(tup._1)
        )

        val commands = exclusions.map(command)

        commands.foreach { cmdString =>
          if config.dryRun then println(cmdString)
          else os.call(cmdString)
        }
        println("done")
