package net.mikolak.pomisos.process

import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

import net.mikolak.pomisos.prefs.Command.SpecEither
import net.mikolak.pomisos.prefs.{Execution, Script}

import scala.collection.JavaConverters._

class ProcessManager {

  import shapeless._

  private object toProcess extends Poly1 {
    implicit def caseExecution = at[Execution](ExecutionProcess.apply)
    implicit def caseScript    = at[Script](ScriptProcess.apply)

  }

  def processFor(spec: SpecEither) = spec.map(toProcess).unify

}

import scala.concurrent.Future
import scala.language.postfixOps
import scala.sys.process._

sealed trait CommandProcess {

  def kill(): Future[Unit]

  def create(): Future[Unit]
}

case class ExecutionProcess(execution: Execution) extends CommandProcess {

  import scala.concurrent.ExecutionContext.Implicits.global

  val cmd = execution.cmd.getOrElse("")

  private def running_?() = (s"pgrep '$cmd'" !) == 0

  def kill() = Future(while (running_?()) { s"pkill '$cmd'" ! })

  def create() = Future(if (!running_?()) { s"nohup '$cmd'" ! })

}

case class ScriptProcess(script: Script) extends CommandProcess {

  import scala.concurrent.ExecutionContext.Implicits.global

  private def runScript(scriptText: Option[String]): Future[Unit] =
    scriptText
      .map(text =>
        Future {
          val tempFile =
            Files.createTempFile(
              "pomisos",
              "script",
              PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr--r--"))) //execute perm
          Files.write(tempFile, text.split("\n").toIterable.asJava)
          tempFile
        }.map(f => { f.toAbsolutePath.toString !; (); }))
      .getOrElse(Future.successful(()))

  override def kill(): Future[Unit] = runScript(script.onPomodoro)

  override def create(): Future[Unit] = runScript(script.onBreak)
}
