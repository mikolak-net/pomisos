import sbt.Keys._

name := "pomisos"

version := "1.0"

scalaVersion := "2.11.8"


resolvers += "jitpack" at "https://jitpack.io"

val gremlinVersion = "3.2.1.0"
val sfxVersion = "0.3"
val macwireVersion = "2.2.4"

libraryDependencies ++= Seq(
  "org.scalafx" %% "scalafx" % "8.0.102-R11",
  "org.scalafx" %% "scalafxml-core-sfx8" % sfxVersion,
  "org.scalafx" %% "scalafxml-macwire-sfx8" % sfxVersion,
  "org.controlsfx" % "controlsfx" % "8.40.12",
  "com.typesafe.akka" %% "akka-actor" % "2.4.11",
  "com.michaelpollmeier" %% "gremlin-scala" % gremlinVersion,
  "com.michaelpollmeier" % "orientdb-gremlin" % gremlinVersion,
  "com.softwaremill.macwire" %% "macros" % macwireVersion % "provided",
  "com.softwaremill.macwire" %% "util" % macwireVersion
)

mainClass in assembly := Some("net.mikolak.pomisos.main.App")
assemblyJarName in assembly := "pomisos.jar"
assemblyMergeStrategy in assembly := {
  case x if x.endsWith("groovy-release-info.properties") => MergeStrategy.first
  case x => (assemblyMergeStrategy in assembly).value(x)
}


addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)