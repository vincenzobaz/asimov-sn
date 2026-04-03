package asimovsn

import fastparse._

class RuleParserTest extends munit.FunSuite:

  // Helper to run the parser and return the result for assertions
  def parse(input: String): Seq[Rule] =
    fastparse.parse(input, p => Rules.file(using p)) match {
      case Parsed.Success(value, _) => value
      case f: Parsed.Failure        =>
        fail(s"Parsing failed at index ${f.index}: ${f.trace().aggregateMsg}")
    }

  test("parse SENT, SIMP, and FULL correctly") {
    val input =
      """SENT /src/a /dst/b
        |SIMP /only/one
        |FULL /absolute/path
        |""".stripMargin

    val expected = Seq(
      Rule.SentilDir("/src/a", "/dst/b"),
      Rule.DirOnly("/only/one"),
      Rule.FullPath(os.Path("/absolute/path").toNIO)
    )

    assertEquals(parse(input), expected)
  }

  test("ignore comments and empty lines") {
    val input =
      """# This is a header
        |SENT /a /b # inline comment
        |
        |   # indented comment
        |FULL /c
        |""".stripMargin

    val result = parse(input)
    assertEquals(result.length, 2)
    assertEquals(result.head, Rule.SentilDir("/a", "/b"))
    assertEquals(result.last, Rule.FullPath(os.Path("/c").toNIO))
  }

  test("handle tabs and multiple spaces") {
    val input = "SENT\t/src/path    /dst/path\n"
    val result = parse(input)
    assertEquals(result, Seq(Rule.SentilDir("/src/path", "/dst/path")))
  }

  test("fail on malformed lines") {
    val input = "SENT /missing/destination"
    // We expect a failure here because SENT requires two paths
    intercept[munit.FailException] {
      parse(input)
    }
  }

  test("parse file") {
    val rules = Rules.process(
      os.read(os.pwd / "src" / "main" / "resources" / "exclusion_rules.txt")
    )
    println(rules)
    assertEquals(rules.length, 38)
  }
