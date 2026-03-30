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
  private[asimovsn] def ws(using P[?]) = P(
    CharsWhile(c => c == ' ' || c == '\t', min = 0)
  )
  private[asimovsn] def comment(using P[?]) = P(
    "#" ~ CharsWhile(c => c != '\n' && c != '\r', min = 0)
  )
  private[asimovsn] def content(using P[?]) = P(
    CharsWhile(c => !c.isWhitespace && c != '#').!
  ).map(_.trim)

  private[asimovsn] def lineEnd(using P[?]) = P(
    ws ~ comment.? ~ ("\n" | "\r\n")
  )
  private[asimovsn] def sentinelDirective(using P[?]) =
    P("SENT" ~ ws ~ content ~ ws ~ content ~ ws)
      .map(Rule.SentilDir.apply)

  private[asimovsn] def dirNameDirective(using P[?]) =
    P("SIMP" ~ ws ~ content).map(Rule.DirOnly.apply)

  private[asimovsn] def fullpathDirective(using P[?]) =
    P("FULL" ~ ws ~ content ~ ws).map { (p: String) =>
      val parsed = os.Path(p)
      // assert(os.isDir(parsed))
      Rule.FullPath(parsed.toNIO)
    }

  private[asimovsn] def blankOrComment(using P[?]) =
    P(ws ~ comment.? ~ lineEnd).map(_ => None)

  private[asimovsn] def validCommand(using P[?]) =
    P(
      ws ~ (sentinelDirective | dirNameDirective | fullpathDirective) ~ comment.? ~ lineEnd
    ).map(Some(_))

  private[asimovsn] def anyLine(using P[?]) = P(validCommand | blankOrComment)

  private[asimovsn] def file(using P[?]) = P(anyLine.rep ~ End).map(_.flatten)

  def process(input: String) =
    parse(input, p => file(using p)) match
      case Parsed.Success(records, _) => records.toList
      case f: Parsed.Failure          =>
        throw new Exception(s"Parse Error: ${f.trace().aggregateMsg}")
