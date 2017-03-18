package net.mikolak.pomisos.process

import akka.actor.{Actor, ActorLogging}
import net.mikolak.pomisos.process.ExecutionLauncher.ErrorInfo
import net.mikolak.pomisos.reporting.Error

trait Launcher { this: Actor with ActorLogging =>

  val errorReceive: Receive = {
    case e: ErrorInfo => reportError(e._2)
  }

  protected def main: Receive

  override def receive: Receive = main.orElse(errorReceive)

  protected final def reportError(message: String) = {
    log.warning(message)
    context.system.eventStream.publish(Error(message))
  }

}
