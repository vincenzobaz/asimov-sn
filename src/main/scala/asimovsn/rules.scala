package asimovsn
import java.nio.file.Path
import scala.jdk.StreamConverters._
import fastparse._, NoWhitespace._

enum Rule:
  case SentilDir(dirName: String, sentinel: String)
  case DirOnly(dirName: String)
  case FullPath(p: Path)

  def matches(p: Path): Option[Path] = this match
    case SentilDir(dirName, sentinel) =>
      Option.when(p.endsWith(sentinel))(p.getParent.resolve(dirName))
    case DirOnly(dirName) =>
      Option
        .when(p.getFileName().toString == dirName)(p.getParent.resolve(dirName))
    case FullPath(fp) =>
      val ab = p.toAbsolutePath()
      Option.when(ab.endsWith(fp) && fp.endsWith(ab))(ab)

object Rules:
  private[asimovsn] object parser {
    type Ctx = P[?]
    def ws(using Ctx) = P(
      CharsWhile(c => c == ' ' || c == '\t', min = 0)
    )
    def comment(using Ctx) = P(
      "#" ~ CharsWhile(c => c != '\n' && c != '\r', min = 0)
    )
    def content(using Ctx) = P(
      CharsWhile(c => !c.isWhitespace && c != '#').!
    ).map(_.trim)

    def lineEnd(using Ctx) = P(
      ws ~ comment.? ~ ("\n" | "\r\n")
    )
    def sentinelDirective(using Ctx) =
      P("SENT" ~ ws ~ content ~ ws ~ content ~ ws)
        .map(Rule.SentilDir.apply)

    def dirNameDirective(using Ctx) =
      P("SIMP" ~ ws ~ content).map(Rule.DirOnly.apply)

    def fullpathDirective(using Ctx) =
      P("FULL" ~ ws ~ content ~ ws).map { (p: String) =>
        val parsed = os.Path(p)
        // assert(os.isDir(parsed))
        Rule.FullPath(parsed.toNIO)
      }

    def blankOrComment(using Ctx) =
      P(ws ~ comment.? ~ lineEnd).map(_ => None)

    def validCommand(using Ctx) =
      P(
        ws ~ (sentinelDirective | dirNameDirective | fullpathDirective) ~ comment.? ~ lineEnd
      ).map(Some(_))

    def anyLine(using Ctx) = P(validCommand | blankOrComment)

    def file(using Ctx) = P(anyLine.rep ~ End).map(_.flatten)
  }

  def process(input: String) =
    parse(input, p => parser.file(using p)) match
      case Parsed.Success(records, _) => records.toList
      case f: Parsed.Failure          =>
        throw Exception(s"Parse Error: ${f.trace().aggregateMsg}")
