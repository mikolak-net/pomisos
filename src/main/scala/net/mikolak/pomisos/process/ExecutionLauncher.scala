package net.mikolak.pomisos.process

import akka.actor.{Actor, ActorLogging, Props}
import net.mikolak.pomisos.prefs.Execution
import scala.sys.process._
import language.postfixOps
import akka.actor.Status.Success

class ExecutionLauncher(execution: Execution) extends Actor with ActorLogging with Launcher with CommandExecution {

  import ExecutionLauncher._

  private val cmd = execution.cmd

  private def running_?() = cmd.exists(c => s"pgrep -f $c".!(devnullProcessLogger) == 0)

  protected def main: Receive = {
    case OnBreak =>
      if (!running_?()) {
        cmd.foreach(c => {
          log.debug(s"Launching command: $c")
          context.actorOf(Props[DeferredExecutor]) ! (s"nohup $c", CodesToIgnore)
        })
      }
    case OnPomodoro =>
      var killsLeft = KillsToTry
      log.debug(s"Attempting to kill process for: $cmd")
      while (running_?() && killsLeft > 0) {
        cmd.foreach { c =>
          execute(s"pkill -f $c")
          killsLeft -= 1
        }
      }
      if (running_?() && killsLeft == 0) {
        reportError(s"Failed to kill process $cmd after $KillsToTry attempts.")
      } else {
        log.debug(s"Successfully killed process for: $cmd")
      }
  }

}

object ExecutionLauncher {

  type ErrorInfo = (Int, String)

  type CommandStatus = Either[ErrorInfo, Success]

  val KillsToTry = 10

  private val RootSigReturnCode = 128
  private val SigTermCode       = RootSigReturnCode + 15
  private val KillCode          = RootSigReturnCode + 9
  val CodesToIgnore             = Set(SigTermCode, KillCode)

  def devnullProcessLogger = ProcessLogger(_ => (), _ => ())
}
