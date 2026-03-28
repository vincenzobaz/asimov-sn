package asimovsn

import java.nio.file.Path
import scala.annotation.tailrec

object AsimovSN:
  @tailrec
  def simplify(acc: List[Path], toConsume: List[Path]): List[Path] =
    toConsume match
      case Nil    => acc
      case l :: t =>
        if acc.exists(p => l.startsWith(p)) then simplify(acc, t)
        else simplify(l :: acc, t)

  def command(p: Path) =
    s"tmutil addexclusion ${p.toAbsolutePath().toString()}"

  def algorithm(config: Config) =
    println("starting")
    val files = os.walk(config.basePath).map(_.toNIO)

    val occurrences = files
      .map(p => config.rules.find(_.matches(p)).map(r => (p, r)))
      .filter(_.nonEmpty)
      .map(_.get)

    println("finding all folders to exclude")
    val exclusions = occurrences
      .map((sentinelPath, tup) => sentinelPath.getParent.resolve(tup._1))
      .toList

    println("simplifying paths")
    val simplified =
      simplify(Nil, exclusions.sortBy(p => os.Path(p).segmentCount))

    println("executing")
    val commands = simplified.map(command)

    commands.foreach { cmdString =>
      if config.dryRun then println(cmdString)
      else os.call(cmdString)
    }
    println("done")

  def main(args: Array[String]): Unit =
    Config.parse(args) match
      case None         => println("failed to parse")
      case Some(config) => algorithm(config)
