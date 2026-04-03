lazy val scalaV = "3.3.6" // A Long Term Support version.

import scala.scalanative.build._
import scala.sys.process._

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
val githubRelease = taskKey[Unit]("Creates or updates the GitHub release for the native binary")
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
      githubRelease := {
        val v = version.value
        val tag = s"v$v"
        val log = streams.value.log
        val binary = buildBinary.value
        val asset = target.value / s"asimov-$v"

        if (!binary.exists()) sys.error(s"Native binary not found: ${binary.getAbsolutePath}")

        IO.copyFile(binary, asset)

        def runOrFail(cmd: Seq[String], errorMessage: String): Unit = {
          log.info(cmd.mkString(" "))
          val exitCode = cmd.!
          if (exitCode != 0) sys.error(errorMessage)
        }

        val releaseExists =
          Seq("gh", "release", "view", tag).!(ProcessLogger(_ => (), _ => ())) == 0

        if (releaseExists) {
          runOrFail(
            Seq("gh", "release", "upload", tag, asset.getAbsolutePath, "--clobber"),
            s"GitHub release upload failed for $tag"
          )
        } else {
          runOrFail(
            Seq("gh", "release", "create", tag, asset.getAbsolutePath, "--generate-notes", "--title", tag),
            s"GitHub release creation failed for $tag"
          )
        }
      },
      publish := githubRelease.value
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
  pushChanges,
  releaseStepTask(native / publish),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
