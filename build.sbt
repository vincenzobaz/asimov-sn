lazy val scalaV = "3.3.6" // A Long Term Support version.

import scala.scalanative.build._

lazy val commonSetings = Seq(
  scalaVersion := scalaV,
  logLevel := Level.Info,
  nativeConfig ~= { c =>
    c.withLTO(LTO.none) // thin
      .withMode(Mode.debug) // releaseFast
      .withGC(GC.immix) // commix
  },
  libraryDependencies ++= Seq(
    "com.lihaoyi" %%% "os-lib" % "0.11.7",
    "com.github.scopt" %%% "scopt" % "4.1.0"
  )
)

lazy val asimovsn =
  project
    .in(file("."))
    .enablePlugins(ScalaNativePlugin)
    .settings(commonSetings)

val release = taskKey[File]("Builds the binary in Release Full mode")
lazy val asimovsnAppleSilicon =
  project
    .in(file("target/appleSilicon"))
    .enablePlugins(ScalaNativePlugin)
    .settings(commonSetings)
    .settings(
      name := "asimovsn-apple-silicon",
      Compile / unmanagedSourceDirectories := (asimovsn / Compile / unmanagedSourceDirectories).value,
      nativeConfig ~= { _.withTargetTriple("arm64-apple-darwin25.4.0") },
      release := (Compile / nativeLinkReleaseFull).value
    )
