package asimovsn

import java.nio.file.Path
import fastparse._, NoWhitespace._

object TmUtil:
  def getSystemExclusions(): List[Path] =
    val res: os.CommandResult = os.call(cmd =
      (
        "defaults",
        "read",
        "/Library/Preferences/com.apple.TimeMachine",
        "SkipPaths"
      )
    )
    assert(res.exitCode == 0)
    val exclusions = parser.parseOutput(res.out.text())
    println(s"obtained current exclusions from ${res.command.mkString(" ")}")
    exclusions.map(e => Path.of(e))

  def addExclusionCommand(p: Path): os.Shellable =
    List("tmutil", "addexclusion", p.toAbsolutePath().toString())

  def removeExclusionCommand(p: Path): os.Shellable =
    List("tmutil", "removeexclusion", p.toAbsolutePath().toString())

  def applyExclusions(exclusions: List[Path], dryRun: Boolean = true) =
    // Get exclusions already applied by system
    val fromSystem = getSystemExclusions()

    // Remove paths that are already excluded
    val newExclusions = exclusions.filter(candidate =>
      !fromSystem.exists(current => candidate.startsWith(current))
    )

    newExclusions.foreach { p =>
      val command = addExclusionCommand(p)
      println(s"running: $command")
      if !dryRun then os.call(command)
    }

  private[asimovsn] object parser:
    type Ctx = P[?]

    def ws(using Ctx) = P(CharsWhile(c => c.isWhitespace, min = 0))
    def comma(using Ctx) = P(ws ~ "," ~ ws)

    def quotedString(using Ctx) = P(
      "\"" ~ CharsWhile(_ != '\"').! ~ "\""
    )

    def unquotedString(using Ctx) = P(
      CharsWhile(c => !c.isWhitespace && c != ',' && c != ')' && c != '(').!
    )

    def pathEntry(using Ctx) = P(quotedString | unquotedString)

    // array parser: ( path1, path2, path3 )
    def arrayParser(using Ctx) = P(
      ws ~ "(" ~ ws ~
        pathEntry.rep(sep = comma) ~
        ws ~ ",".? ~ ws ~ // Apple sometimes includes a trailing comma before the ')'
        ")" ~ ws ~ End
    )

    def parseOutput(input: String): List[String] = {
      parse(input, p => arrayParser(using p)) match {
        case Parsed.Success(value, _) => value.toList
        case f: Parsed.Failure        =>
          throw Exception(
            s"Failed to parse TimeMachine paths: ${f.trace().aggregateMsg}"
          )
      }
    }
  end parser
