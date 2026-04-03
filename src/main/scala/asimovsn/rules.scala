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
  // 1. Helper: A "string" is everything until a comma or newline
  private def comment(using P[?]) = P(
    "#" ~ CharsWhile(c => c != '\n' && c != '\r', min = 0)
  )
  private def content(using P[?]) = P(
    CharsWhile(c => c != ',' && c != '\n' && c != '\r' && c != '#', min = 1).!
  ).map(_.trim)

  // 2. Individual Line Parsers
  private def lineEnd(using P[?]) = P(
    " ".rep ~ comment.? ~ ("\n" | "\r\n" | End)
  )
  // format: SENT,s1,s2
  private def sentLine(using P[?]) =
    P("SENT" ~ "," ~ content ~ "," ~ content ~ lineEnd)
      .map(Rule.SentilDir.apply)

  // format: SIMP,s1
  private def simpLine(using P[?]) =
    P("SIMP" ~ "," ~ content ~ lineEnd).map(Rule.DirOnly.apply)

  // format: FULL,s1
  private def fullLine(using P[?]) = P("FULL" ~ "," ~ content ~ lineEnd).map {
    (p: String) =>
      val parsed = os.Path(p)
      // assert(os.isDir(parsed))
      Rule.FullPath(parsed.toNIO)
  }

  // 3. Combined Line Parser (Try each one)
  private def anyLine(using P[?]) =
    P((sentLine | simpLine | fullLine).map(Some(_))) | P(
      (comment.? ~ ("\n" | "\r\n")).map(_ => None) | (comment ~ End).map(_ =>
        None
      )
    )

  // 4. The whole file (lines separated by newlines)
  private def file(using P[?]) = P(anyLine.rep(1) ~ End).map(_.flatten)

  def process(input: String) =
    parse(input, p => file(using p)) match
      case Parsed.Success(records, _) => records.toList
      case f: Parsed.Failure          =>
        throw new Exception(s"Parse Error: ${f.trace().aggregateMsg}")

  val default: List[Rule] = List(
    (".build", "Package.swift"), // Swift
    (".gradle", "build.gradle"), // Gradle
    (".gradle", "build.gradle.kts"), // Gradle Kotlin Script
    ("build", "build.gradle"), // Gradle build files
    ("build", "build.gradle.kts"), // Gradle Kotlin Script build files
    (".dart_tool", "pubspec.yaml"), // Flutter (Dart)
    (".packages", "pubspec.yaml"), // Pub (Dart)
    (".stack-work", "stack.yaml"), // Stack (Haskell)
    (".tox", "tox.ini"), // Tox (Python)
    (".nox", "noxfile.py"), // Nox (Python)
    (".vagrant", "Vagrantfile"), // Vagrant
    (".venv", "requirements.txt"), // virtualenv (Python)
    (".venv", "pyproject.toml"), // virtualenv (Python)
    ("Carthage", "Cartfile"), // Carthage
    ("Pods", "Podfile"), // CocoaPods
    ("bower_components", "bower.json"), // Bower (JavaScript)
    ("build", "build.gradle"), // Gradle
    ("build", "build.gradle.kts"), // Gradle Kotlin Script
    ("build", "pubspec.yaml"), // Flutter (Dart)
    ("build", "setup.py"), // Python
    ("dist", "setup.py"), // PyPI Publishing (Python)
    ("node_modules", "package.json"), // npm, Yarn (NodeJS)
    (".parcel-cache", "package.json"), // Parcel v2 cache (JavaScript)
    ("target", "Cargo.toml"), // Cargo (Rust)
    ("target", "pom.xml"), // Maven
    ("target", "build.sbt"), // Sbt (Scala)
    ("target", "plugins.sbt"), // Sbt plugins (Scala)
    ("vendor", "composer.json"), // Composer (PHP)
    ("vendor", "Gemfile"), // Bundler (Ruby)
    ("vendor", "go.mod"), // Go Modules (Golang)
    ("venv", "requirements.txt"), // virtualenv (Python)
    ("deps", "mix.exs"), // Mix dependencies (Elixir)
    (".build", "mix.exs"), // Mix build files (Elixir)
    (".terraform.d", ".terraformrc"), // Terraform plugin cache
    (".terragrunt-cache", "terragrunt.hcl"), // Terragrunt
    ("cdk.out", "cdk.json") // AWS CDK
  ).map((d, r) => Rule.SentilDir(d, r)) ++ List(
    Rule.DirOnly(".vscode")
  )
