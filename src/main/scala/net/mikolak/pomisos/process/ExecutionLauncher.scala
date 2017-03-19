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
    case OnBreak => if (!running_?()) { cmd.foreach(c => context.actorOf(Props[DeferredExecutor]) ! s"nohup $c") }
    case OnPomodoro =>
      var killsLeft = KillsToTry
      while (running_?() && killsLeft > 0) {
        cmd.foreach { c =>
          execute(s"pkill -f $c")
          killsLeft -= 1
        }
      }
      if (running_?() && killsLeft == 0) {
        reportError(s"Failed to kill process $cmd after $KillsToTry attempts.")
      }
  }

}

object ExecutionLauncher {

  type ErrorInfo = (Int, String)

  type CommandStatus = Either[ErrorInfo, Success]

  val KillsToTry = 10

  def devnullProcessLogger = ProcessLogger(_ => (), _ => ())
}
