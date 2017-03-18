package net.mikolak.pomisos.dependencies

import akka.actor.{ActorRef, Props}
import com.softwaremill.macwire.akkasupport._
import com.softwaremill.macwire._
import com.softwaremill.tagging._
import net.mikolak.pomisos.prefs.{Execution, Script}
import net.mikolak.pomisos.process.{ExecutionLauncher, ProcessManager, ScriptLauncher}

trait ProcessModule { this: AkkaModule with DbModule =>

  lazy val executionLauncher: (Execution) => Props @@ ExecutionLauncher = _ =>
    wireProps[ExecutionLauncher].taggedWith[ExecutionLauncher]
  lazy val scriptLauncher: (Script) => Props @@ ScriptLauncher = _ => wireProps[ScriptLauncher].taggedWith[ScriptLauncher]

  lazy val processManager: ActorRef @@ ProcessManager =
    wireActor[ProcessManager]("processManager").taggedWith[ProcessManager]

  lazy val pmContainer: ActorRefContainer[ProcessManager] = wire[ActorRefContainer[ProcessManager]]

}
