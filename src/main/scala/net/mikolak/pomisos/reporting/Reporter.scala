package net.mikolak.pomisos.reporting

import akka.actor.{Actor, ActorLogging}

import scalafx.application.Platform
import scalafx.scene.control.Alert
import scalafx.scene.control.Alert.AlertType

class Reporter(dialogLauncher: DialogLauncher) extends Actor with ActorLogging {

  override def receive: Receive = {
    case Error(msg)   => dialogLauncher(msg, AlertType.Error)
    case Warning(msg) => dialogLauncher(msg, AlertType.Warning)
  }
}

sealed trait ReportingNotification {
  def msg: String
}

case class Error(msg: String) extends ReportingNotification

case class Warning(msg: String) extends ReportingNotification

trait DialogLauncher {

  def apply(msg: String, severity: AlertType): Unit

}

class DefaultDialogLauncher extends DialogLauncher {

  def apply(msg: String, severity: AlertType): Unit = Platform.runLater {
    new Alert(severity, msg).showAndWait
  }

}
