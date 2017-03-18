package net.mikolak.pomisos.process

import akka.actor.{Actor, OneForOneStrategy, Props, SupervisorStrategy}
import com.softwaremill.tagging.@@
import net.mikolak.pomisos.prefs.{CommandDao, Execution, Script}

class ProcessManager(commandDao: CommandDao,
                     executionLaunchers: (Execution) => Props @@ ExecutionLauncher,
                     scriptLaunchers: (Script) => Props @@ ScriptLauncher)
    extends Actor {

  import shapeless._

  private object toProps extends Poly1 {
    implicit def caseExecution = at[Execution](executionLaunchers)
    implicit def caseScript    = at[Script](scriptLaunchers)

  }

  private def actorsForCurrentCommands = commandDao.getAll().map(_._2.map(toProps).unify).map(context.actorOf)

  override def receive: Receive = {
    case action: ProcessAction =>
      actorsForCurrentCommands.foreach { a =>
        a ! action
      }
  }
  override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy() { case _ => SupervisorStrategy.Stop }
}

sealed trait ProcessAction

object OnPomodoro extends ProcessAction

object OnBreak extends ProcessAction
