package net.mikolak.pomisos.process

import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

import akka.actor.{Actor, ActorLogging, Props}
import net.mikolak.pomisos.prefs.Script
import scala.collection.JavaConverters._

class ScriptLauncher(script: Script) extends Actor with ActorLogging with Launcher {
  private def runScript(scriptText: Option[String]) =
    scriptText
      .foreach(text => {
        val tempFile =
          Files.createTempFile("pomisos",
                               "script",
                               PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr--r--"))) //execute perm
        Files.write(tempFile, text.split("\n").toIterable.asJava)
        val command = tempFile.toAbsolutePath.toString
        log.debug(s"Launching script from temporary file $command")
        context.actorOf(Props[DeferredExecutor]) ! (s"nohup $command", Set.empty[Int])
      })

  protected def main: Receive = {
    case OnBreak    => runScript(script.onBreak)
    case OnPomodoro => runScript(script.onPomodoro)
  }
}
