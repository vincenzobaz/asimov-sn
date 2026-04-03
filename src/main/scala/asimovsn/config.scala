package asimovsn

import java.nio.file.Path
import scopt.OParser

case class Config(
    dryRun: Boolean = true,
    rules: List[Rule] = Rules.default,
    basePath: os.Path = os.home
)

object Config:
  def parse(args: Array[String]) =
    val builder = OParser.builder[Config]
    val parser = {
      import builder._
      OParser.sequence(
        programName("asimov-sn"),
        opt[Boolean]('d', "dry-run").text("print exclusions without applying"),
        opt[Path]('r', "--rules")
          .text("List of rules")
          .action((x, c) => c.copy(rules = Rules.parse(os.read(os.Path(x))))),
        opt[Path]('b', "base-path")
          .text("Base path to search")
          .action((x, c) => c.copy(basePath = os.Path(x)))
      )
    }
    OParser.parse(parser, args, Config())
