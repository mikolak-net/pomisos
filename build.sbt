import java.nio.file.Files

import sbt._
import sbt.Keys._
import sbtdynver.impl.NoProcessLogger
import sys.process._

import scala.util.Try

name := "pomisos"
scalaVersion := "2.12.6"

resolvers += "jitpack" at "https://jitpack.io"
resolvers += Resolver.bintrayRepo("jerady", "maven")

val gremlinVersion = "3.2.3."
val sfxVersion     = "0.3"
val macwireVersion = "2.3.0"
val akkaVersion    = "2.4.17"
val logbackVersion = "1.2.2"
val smileVersion   = "1.3.1"

libraryDependencies ++= Seq(
  "org.scalafx"                %% "scalafx"                     % "8.0.102-R11",
  "org.scalafx"                %% "scalafxml-core-sfx8"         % sfxVersion,
  "org.scalafx"                %% "scalafxml-macwire-sfx8"      % sfxVersion,
  "de.jensd"                   % "fontawesomefx-fontawesome"    % "4.7.0-5",
  "com.jfoenix"                % "jfoenix"                      % "8.0.4",
  "org.controlsfx"             % "controlsfx"                   % "8.40.12",
  "com.typesafe.akka"          %% "akka-actor"                  % akkaVersion,
  "com.typesafe.akka"          %% "akka-slf4j"                  % akkaVersion,
  "com.typesafe.akka"          %% "akka-http"                   % "10.0.3",
  "de.heikoseeberger"          %% "akka-http-upickle"           % "1.12.0",
  "com.lihaoyi"                %% "upickle"                     % "0.4.3",
  "com.michaelpollmeier"       %% "gremlin-scala"               % (gremlinVersion + "4"),
  "com.michaelpollmeier"       % "orientdb-gremlin"             % (gremlinVersion + "0"),
  "com.softwaremill.macwire"   %% "macros"                      % macwireVersion % "provided",
  "com.softwaremill.macwire"   %% "util"                        % macwireVersion,
  "com.softwaremill.macwire"   %% "macrosakka"                  % macwireVersion,
  "com.github.haifengl"        %% "smile-scala"                 % smileVersion,
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
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest,
                                      "-u",
                                      sys.env.getOrElse("CIRCLE_TEST_REPORTS", target.value) + "/test-reports")

enablePlugins(JavaAppPackaging, JDKPackagerPlugin)

//sets data dir as a fallback in case something fails in-app
bashScriptExtraDefines +=
  s"""BASE_DATA_PATH=$${XDG_DATA_HOME:-$$HOME/.local/share}
     |DATA_PATH=$$BASE_DATA_PATH/${name.value}
     |mkdir -p $$DATA_PATH
     |cd $$DATA_PATH
  """.stripMargin

maintainer := "Mikołaj Koziarkiewicz"
packageSummary := "Pomisos Pomodoro App"
packageDescription := "A pomodoro app with several cool features"

maintainer in JDKPackager := "mikolak.net"
version in JDKPackager := {
  val baseDynVersion = (version in Compile).value.replaceAll("\\-", ".")
  if (!baseDynVersion.matches("^[0-9].*")) {
    s"0.$baseDynVersion"
  } else {
    baseDynVersion
  }
}

lazy val iconGlob = sys.props("os.name") match {
  case os if os.contains("Mac OS") => "icon.icns"
  case os if os.contains("Win")    => "icon.ico"
  case _                           => "icon.png"
}

jdkAppIcon := (sourceDirectory.value ** iconGlob).getPaths.headOption.map(file)
jdkPackagerType := "all"
jdkPackagerJVMArgs := Seq("-Xmx512m")
jdkPackagerProperties := Map("app.name"      -> name.value,
                             "app.version"   -> version.value,
                             "info.license"  -> "Apache License, Version 2.0",
                             "info.category" -> "Office")
jdkPackagerAppArgs := Seq(maintainer.value, packageSummary.value, packageDescription.value)

//rewrite ant task to actually use the info keys
antBuildDefn in JDKPackager := {
  val origTask = (antBuildDefn in JDKPackager).value

  val InfoLabel = "info"
  val KeyRegex  = s"$InfoLabel\\.(.+)".r

  import scala.xml._
  import scala.xml.transform._
  val infoRewrite = new RewriteRule {
    override def transform(n: Node) = n match {
      case e: Elem if e.prefix == "fx" && e.label == InfoLabel =>
        val attribMap = jdkPackagerProperties.value.collect {
          case (KeyRegex(infoKey), value) =>
            (infoKey, value)
        }

        attribMap.foldRight(e) {
          case ((key, value), infoElem) => infoElem % Attribute("", key, value, Null)
        }
      case other => other
    }
  }

  new RuleTransformer(infoRewrite)(origTask)
}

makeIcons := {
  val inputFileName  = "icon.svg"
  val inputName      = inputFileName.split('.').head
  val inputFile      = new File(inputFileName)
  val outputBasePath = (resourceDirectory in Compile).value
  val log            = streams.value.log

  case class ImgSpec(suffix: String, size: Int, format: String) {
    val outputFile = outputBasePath / s"$inputName$suffix.$format"
  }

  val commandChecks = List("inkscape --version", "convert", "png2icns")

  def generateCmd(spec: ImgSpec)(outFile: File = spec.outputFile): List[List[String]] =
    if (spec.format == "png") {
      List(
        List(
          "inkscape",
          s"--file=${baseDirectory.value / inputFileName}",
          s"--export-${spec.format}=${outFile.absolutePath}",
          s"-w${spec.size}",
          s"-h${spec.size}",
          "--export-area-page"
        ))
    } else {
      val tempExtension = "png"
      val tempFile      = Files.createTempFile("", s".$tempExtension").toFile
      Files.delete(tempFile.toPath)

      val otherCommand = if (spec.format == "icns") {
        List("png2icns", spec.outputFile.absolutePath, tempFile.absolutePath)
      } else {
        List("convert", tempFile.absolutePath, spec.outputFile.absolutePath)
      }

      generateCmd(spec.copy(format = tempExtension))(tempFile) :+
        otherCommand
    }

  val outputFiles = List(ImgSpec("_small", 24, "png"),
                         ImgSpec("", 64, "png"),
                         ImgSpec("_large", 128, "png"),
                         ImgSpec("", 64, "ico"),
                         ImgSpec("", 128, "icns"))

  val filesToRefresh = outputFiles.filter { spec =>
    val out = spec.outputFile
    !out.exists() || (out.lastModified < inputFile.lastModified)
  }

  def doRefresh() = {
    import sys.process._
    import language.postfixOps

    for (spec <- filesToRefresh) yield {
      val out = spec.outputFile

      generateCmd(spec)().foreach(cmd => log.info(Process(cmd) !!))

      out
    }
  }

  if (commandChecks.forall(cmd => Try(cmd.!(NoProcessLogger)).isSuccess)) {
    doRefresh()
  } else {
    if (filesToRefresh.nonEmpty) {
      log.warn(
        s"Icons need to be updated, but either of ${commandChecks.mkString(", ")} is missing. Install to allow regeneration.")
    }
    Seq.empty[File]
  }
}
