import sbt._
import sbt.Keys._

name := "pomisos"
version := "1.0"
scalaVersion := "2.11.8"

resolvers += "jitpack" at "https://jitpack.io"

val gremlinVersion = "3.2.3."
val sfxVersion     = "0.3"
val macwireVersion = "2.2.4"

libraryDependencies ++= Seq(
  "org.scalafx"                %% "scalafx"                % "8.0.102-R11",
  "org.scalafx"                %% "scalafxml-core-sfx8"    % sfxVersion,
  "org.scalafx"                %% "scalafxml-macwire-sfx8" % sfxVersion,
  "org.controlsfx"             % "controlsfx"              % "8.40.12",
  "com.typesafe.akka"          %% "akka-actor"             % "2.4.11",
  "com.michaelpollmeier"       %% "gremlin-scala"          % (gremlinVersion + "1"),
  "com.michaelpollmeier"       % "orientdb-gremlin"        % (gremlinVersion + "0"),
  "com.softwaremill.macwire"   %% "macros"                 % macwireVersion % "provided",
  "com.softwaremill.macwire"   %% "util"                   % macwireVersion,
  "com.github.haifengl"        % "smile-core"              % "1.2.0",
  "com.github.haifengl"        % "smile-math"              % "1.2.0",
  "com.github.haifengl"        %% "smile-scala"            % "1.2.0",
  "com.softwaremill.quicklens" %% "quicklens"              % "1.4.8",
  "com.chuusai"                %% "shapeless"              % "2.3.2"
)

mainClass in assembly := Some("net.mikolak.pomisos.main.App")
assemblyJarName in assembly := "pomisos.jar"
assemblyMergeStrategy in assembly := {
  case x if x.endsWith("groovy-release-info.properties") => MergeStrategy.first
  case x if x.contains("xmlpull")                        => MergeStrategy.first
  case x                                                 => (assemblyMergeStrategy in assembly).value(x)
}

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

scalacOptions += "-P:clippy:colors=true"

val makeIcons = taskKey[Seq[File]]("make them icons")

//TODO: temporarily disable resourceGenerators in Compile += makeIcons.taskValue
fork in run := true //so that OrientDB runs correctly

makeIcons := {
  val inputFile = "icon.svg"

  val inputName = inputFile.split('.').head

  val outputBasePath = (resourceDirectory in Compile).value

  val outputFiles = List(("_small", 24, 24), ("", 64, 64))

  import sys.process._
  import language.postfixOps

  for ((suffix, width, height) <- outputFiles) yield {
    val outputFile = outputBasePath / s"$inputName$suffix.png"
    val cmd =
      Process("inkscape",
              List(s"--file=${baseDirectory.value / inputFile}",
                   s"--export-png=${outputFile}",
                   s"-w$width",
                   s"-h$height",
                   "--export-area-page"))
    streams.value.log.info(cmd !!)
    outputFile
  }
}
