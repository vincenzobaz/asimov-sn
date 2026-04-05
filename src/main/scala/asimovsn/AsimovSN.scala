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

  def createExclusionList(config: Config): List[Path] =
    println("starting exclusion search")
    val files = os.walk.stream(config.basePath).map(_.toNIO)

    val occurrences = files
      .flatMap(p => config.rules.map(_.matches(p)))
      .collect { case Some(r) => r }

    println("finding all folders to exclude")
    val exclusions = occurrences.toList

    println("simplifying paths")
    val simplified =
      simplify(Nil, exclusions.sortBy(p => os.Path(p).segmentCount))

    println("done with exclusion search")
    simplified

  def main(args: Array[String]): Unit =
    Config.parse(args) match
      case None         => println("failed to parse")
      case Some(config) =>
        println(s"parsed config: $config")
        val exclusions = createExclusionList(config)

        config.output match
          case Output.Restic =>
            val target = os.pwd / "exclusions.txt"
            os.write(target, exclusions.mkString("\n"))
            println(s"wrote exclusions to $target")
          case Output.TimeMachine =>
            TmUtil.applyExclusions(exclusions, dryRun = config.dryRun)
