package net.mikolak.pomisos.process

import akka.actor.ActorLogging
import akka.actor.Status.Success
import net.mikolak.pomisos.process.ExecutionLauncher.CommandStatus

import scala.language.postfixOps
import scala.sys.process._
import scala.util.Try

trait CommandExecution { this: ActorLogging =>
  protected final def execute(fullCommand: String): CommandStatus = {
    log.debug(s"Attempting to execute command $fullCommand")

    val stdOut = new StringBuffer()
    val stdErr = new StringBuffer()

    def processLogger = ProcessLogger(stdOut.append(_: String), stdErr.append(_: String))

    try {
      Right(Success(fullCommand.!!(processLogger)))
    } catch {
      case e: RuntimeException if e.getMessage.startsWith("Nonzero exit value: ") =>
        val errorCode = Try(e.getMessage.split(" ").last.toInt).toOption.getOrElse(-1)
        val messageText =
          s"""Command $fullCommand failed with error code: $errorCode.
             |Error output is as follows:
             |$stdErr
             |------
             |Standard output is as follows:
             |$stdOut
             |------
           """.stripMargin
        Left((errorCode, messageText))
    }
  }
}
