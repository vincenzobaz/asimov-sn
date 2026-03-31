lazy val scalaV = "3.3.6" // A Long Term Support version.

import scala.scalanative.build._

lazy val commonSetings = Seq(
  scalaVersion := scalaV,
  logLevel := Level.Info,
  libraryDependencies ++= Seq(
    "com.lihaoyi" %%% "os-lib" % "0.11.7",
    "com.github.scopt" %%% "scopt" % "4.1.0",
    "com.lihaoyi" %%% "fastparse" % "3.1.1",
    "org.scalameta" %% "munit" % "1.0.4" % Test
  )
)

// Core project, develop using jdk
lazy val asimov =
  project
    .in(file("."))
    .settings(commonSetings)

val buildBinary = taskKey[File]("Builds the binary in Release Full mode")
lazy val native =
  project
    .in(file("target/native"))
    .enablePlugins(ScalaNativePlugin, ReleasePlugin)
    .settings(commonSetings)
    .settings(
      name := "asimovsn-apple-silicon",
      Compile / unmanagedSourceDirectories := (asimov / Compile / unmanagedSourceDirectories).value,
      buildBinary := (Compile / nativeLinkReleaseFull).value,
      nativeConfig ~= { c =>
        c.withLTO(LTO.none) // thin
          .withMode(Mode.debug) // releaseFast
          .withGC(GC.immix) // commix
          .withTargetTriple("arm64-apple-darwin25.4.0")
      },
      publishMavenStyle := false,
      publishTo := Some(Resolver.file("dummy", target.value / "out")),
      publish := {
        // override publish to push to Github release
        import scala.sys.process._
        val v = version.value
        val log = streams.value.log
        val binary = (Compile / nativeLinkReleaseFull).value.toString

        log.info(s"Releasing v$v to GitHub...")

        val exitCode =
          Seq("gh", "release", "create", s"v$v", binary, "--generate-notes").!
        if (exitCode != 0) sys.error("GitHub release failed!")
      }
    )

import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._

ThisBuild / versionScheme := Some("early-semver")
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("native/publish"), 
  setNextVersion,
  commitNextVersion,
  pushChanges
)