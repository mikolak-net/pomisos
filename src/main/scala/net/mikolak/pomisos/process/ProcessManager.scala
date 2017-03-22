package net.mikolak.pomisos.process

import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props, SupervisorStrategy}
import com.softwaremill.tagging.@@
import net.mikolak.pomisos.prefs.{CommandDao, Execution, Script}

class ProcessManager(commandDao: CommandDao,
                     executionLaunchers: (Execution) => Props @@ ExecutionLauncher,
                     scriptLaunchers: (Script) => Props @@ ScriptLauncher)
    extends Actor
    with ActorLogging {

  import shapeless._

  private object toProps extends Poly1 {
    implicit def caseExecution = at[Execution](executionLaunchers)
    implicit def caseScript    = at[Script](scriptLaunchers)

  }

  private def actorsForCurrentCommands = commandDao.getAll().map(_._2.map(toProps).unify).map(context.actorOf)

  override def receive: Receive = {
    case action: ProcessAction =>
      logProcesses(action)
      actorsForCurrentCommands.foreach { a =>
        a ! action
      }
  }

  private def logProcesses(action: ProcessAction) = {
    lazy val actionType = action match {
      case OnBreak    => "break"
      case OnPomodoro => "pomodoro"
    }

    lazy val processMap = commandDao.getAll().map(_._2.unify).groupBy(_.getClass.getSimpleName).mapValues(_.size)

    log.info(s"Performing ${processMap.values.sum} executions/scripts for $actionType; types: (${processMap.mkString(", ")})")
    log.debug(s"Launching executions and scripts: ${commandDao.getAll}")

  }

  override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy() { case _ => SupervisorStrategy.Stop }
}

sealed trait ProcessAction

object OnPomodoro extends ProcessAction

object OnBreak extends ProcessAction
