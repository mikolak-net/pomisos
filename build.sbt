import sbt._
import sbt.Keys._

import scala.util.Try

name := "pomisos"
version := "0.8.alpha"
scalaVersion := "2.11.8"

resolvers += "jitpack" at "https://jitpack.io"

val gremlinVersion = "3.2.3."
val sfxVersion     = "0.3"
val macwireVersion = "2.3.0"
val akkaVersion    = "2.4.17"
val logbackVersion = "1.2.2"

libraryDependencies ++= Seq(
  "org.scalafx"                %% "scalafx"                     % "8.0.102-R11",
  "org.scalafx"                %% "scalafxml-core-sfx8"         % sfxVersion,
  "org.scalafx"                %% "scalafxml-macwire-sfx8"      % sfxVersion,
  "org.controlsfx"             % "controlsfx"                   % "8.40.12",
  "com.typesafe.akka"          %% "akka-actor"                  % akkaVersion,
  "com.typesafe.akka"          %% "akka-slf4j"                  % akkaVersion,
  "com.typesafe.akka"          %% "akka-http"                   % "10.0.3",
  "de.heikoseeberger"          %% "akka-http-upickle"           % "1.12.0",
  "com.lihaoyi"                %% "upickle"                     % "0.4.3",
  "com.michaelpollmeier"       %% "gremlin-scala"               % (gremlinVersion + "1"),
  "com.michaelpollmeier"       % "orientdb-gremlin"             % (gremlinVersion + "0"),
  "com.softwaremill.macwire"   %% "macros"                      % macwireVersion % "provided",
  "com.softwaremill.macwire"   %% "util"                        % macwireVersion,
  "com.softwaremill.macwire"   %% "macrosakka"                  % macwireVersion,
  "com.github.haifengl"        % "smile-core"                   % "1.2.0",
  "com.github.haifengl"        % "smile-math"                   % "1.2.0",
  "com.github.haifengl"        %% "smile-scala"                 % "1.2.0",
  "com.softwaremill.quicklens" %% "quicklens"                   % "1.4.8",
  "com.chuusai"                %% "shapeless"                   % "2.3.2",
  "com.typesafe.scala-logging" %% "scala-logging"               % "3.5.0",
  "ch.qos.logback"             % "logback-core"                 % logbackVersion,
  "ch.qos.logback"             % "logback-classic"              % logbackVersion,
  "org.slf4j"                  % "jul-to-slf4j"                 % "1.7.25",
  "com.typesafe.akka"          %% "akka-testkit"                % "2.4.17" % "test",
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

enablePlugins(JavaAppPackaging, JDKPackagerPlugin)

maintainer := "Mikołaj Koziarkiewicz"
packageSummary := "Pomisos Pomodoro App"
packageDescription := "A pomodoro app with several cool features"

//sets data dir as a fallback in case something fails in-app
bashScriptExtraDefines +=
  s"""BASE_DATA_PATH=$${XDG_DATA_HOME:-$$HOME/.local/share}
     |DATA_PATH=$$BASE_DATA_PATH/${name.value}
     |mkdir -p $$DATA_PATH
     |cd $$DATA_PATH
  """.stripMargin

rpmVendor := "mikolak.net"
rpmLicense := Some("Apache License, Version 2.0")

lazy val iconGlob = sys.props("os.name") match {
  case os if os.contains("Mac OS") => "icon.icns"
  case os if os.contains("Win")    => "icon.ico"
  case _                           => "icon.png"
}

jdkAppIcon := (sourceDirectory.value ** iconGlob).getPaths.headOption.map(file)
jdkPackagerType := "installer"
jdkPackagerJVMArgs := Seq("-Xmx512m")
jdkPackagerProperties := Map("app.name" -> name.value, "app.version" -> version.value)
jdkPackagerAppArgs := Seq(maintainer.value, packageSummary.value, packageDescription.value)

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
