package asimovsn
import java.nio.file.Path
import scala.jdk.StreamConverters._

case class Rule(directory: String, sentinel: Option[String]):
  def matches(p: Path): Boolean =
    sentinel match
      case Some(sent) => p.endsWith(sent)
      case None       => p.getFileName().toString == directory

object Rules:
  def parse(raw: String): List[Rule] =
    raw
      .lines()
      .map(l => l.split(" ").toList)
      .map {
        case d :: Nil      => Rule(d, None)
        case d :: s :: Nil => Rule(d, Some(s))
        case s             => throw Exception(s"Invalid line $s")
      }
      .toScala(List)

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
  ).map((d, r) => Rule(d, Some(r))) ++ List(
    Rule(".vscode", None)
  )
