package net.mikolak.pomisos.utils

import org.controlsfx.control.Notifications

import scalafx.geometry.Pos
import scalafx.scene.image.ImageView
import scala.concurrent.duration._
import language.postfixOps
import Implicits._
import net.mikolak.pomisos.dependencies.MainIcon

class Notifications(icon: MainIcon) {

  def show(message: String) = {
        Notifications.create
          .title("Pomodoro")
          .text(message)
          .hideAfter(5 seconds)
          .hideCloseButton()
          .graphic(new ImageView(icon.image))
          .position(Pos.TopRight)
          .show()
  }

}
