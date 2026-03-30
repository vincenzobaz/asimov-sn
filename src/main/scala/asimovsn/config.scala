package asimovsn

import java.nio.file.Path
import scopt.OParser
import scopt.Read

enum Output:
  case Restic
  case TimeMachine

object Output:
  given Read[Output] = Read.reads {
    case "restic"      => Output.Restic
    case "timemachine" => Output.TimeMachine
    case x @ _         => throw Exception(s"invalid output type $x")
  }

case class Config(
    dryRun: Boolean = true,
    rules: List[Rule] = Rules.process(
      os.read(os.pwd / "src" / "main" / "resources" / "exclusion_rules.txt")
    ),
    basePath: os.Path = os.home / "Desktop",
    output: Output = Output.Restic
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
          .action((x, c) => c.copy(rules = Rules.process(os.read(os.Path(x))))),
        opt[Path]('b', "base-path")
          .text("Base path to search")
          .action((x, c) => c.copy(basePath = os.Path(x))),
        opt[Output]('o', "output")
          .text("Type of output: restic|timemachine")
      )
    }
    OParser.parse(parser, args, Config())
