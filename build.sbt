import sbt._
import sbt.Keys._

import scala.util.Try

name := "pomisos"
version := "1.0"
scalaVersion := "2.11.8"

resolvers += "jitpack" at "https://jitpack.io"

val gremlinVersion = "3.2.3."
val sfxVersion     = "0.3"
val macwireVersion = "2.2.4"

libraryDependencies ++= Seq(
  "org.scalafx"                %% "scalafx"                     % "8.0.102-R11",
  "org.scalafx"                %% "scalafxml-core-sfx8"         % sfxVersion,
  "org.scalafx"                %% "scalafxml-macwire-sfx8"      % sfxVersion,
  "org.controlsfx"             % "controlsfx"                   % "8.40.12",
  "com.typesafe.akka"          %% "akka-actor"                  % "2.4.17",
  "com.typesafe.akka"          %% "akka-http"                   % "10.0.3",
  "de.heikoseeberger"          %% "akka-http-upickle"           % "1.12.0",
  "com.lihaoyi"                %% "upickle"                     % "0.4.3",
  "com.michaelpollmeier"       %% "gremlin-scala"               % (gremlinVersion + "1"),
  "com.michaelpollmeier"       % "orientdb-gremlin"             % (gremlinVersion + "0"),
  "com.softwaremill.macwire"   %% "macros"                      % macwireVersion % "provided",
  "com.softwaremill.macwire"   %% "util"                        % macwireVersion,
  "com.github.haifengl"        % "smile-core"                   % "1.2.0",
  "com.github.haifengl"        % "smile-math"                   % "1.2.0",
  "com.github.haifengl"        %% "smile-scala"                 % "1.2.0",
  "com.softwaremill.quicklens" %% "quicklens"                   % "1.4.8",
  "com.chuusai"                %% "shapeless"                   % "2.3.2",
  "org.scalatest"              %% "scalatest"                   % "3.0.1" % "test",
  "org.mockito"                % "mockito-all"                  % "1.9.0" % "test",
  "org.scalacheck"             %% "scalacheck"                  % "1.13.4" % "test",
  "com.fortysevendeg"          %% "scalacheck-toolbox-datetime" % "0.2.1" % "test"
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

resourceGenerators in Compile += makeIcons.taskValue
fork in run := true //so that OrientDB runs correctly

makeIcons := {
  val generatorCmd = "inkscape"

  val inputFileName = "icon.svg"
  val inputFile     = new File(inputFileName)

  val inputName = inputFileName.split('.').head

  val outputFiles = List(("_small", 24, 24), ("", 64, 64))

  val outputBasePath = (resourceDirectory in Compile).value

  val filesToRefresh = {
    val filesWithTargets = outputFiles.map { configTuple =>
      (outputBasePath / s"$inputName${configTuple._1}.png", configTuple._2, configTuple._3)
    }

    filesWithTargets.filter { config =>
      val out = config._1
      !out.exists() || (out.lastModified < inputFile.lastModified)
    }
  }

  def doRefresh() = {
    import sys.process._
    import language.postfixOps

    for ((out, width, height) <- filesToRefresh) yield {
      val cmd =
        Process(
          generatorCmd,
          List(s"--file=${baseDirectory.value / inputFileName}",
               s"--export-png=$out",
               s"-w$width",
               s"-h$height",
               "--export-area-page")
        )
      streams.value.log.info(cmd !!)
      out
    }
  }

  if (Try(s"$generatorCmd --version").isSuccess) {
    doRefresh()
  } else {
    if (filesToRefresh.nonEmpty) {
      streams.value.log
        .warn(s"Icons need to be updated, but $generatorCmd is missing. Install $generatorCmd to allow regeneration.")
    }
    Seq.empty[File]
  }
}
