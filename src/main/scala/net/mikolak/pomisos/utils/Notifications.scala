package net.mikolak.pomisos.utils

import notify.{Notify => NotificationLauncher}
import org.controlsfx.control.Notifications

class Notifications {

  def show(message: String) = {

    NotificationLauncher.notify("pomodoro", message)
    //TODO: WERYFIKACJA
    //    Notifications.create
    //      .title("Pomodoro")
    //      .text(message)
    //      .hideAfter(5 seconds)
    //      .hideCloseButton()
    //      .graphic(new ImageView(icon.image))
    //      .position(Pos.BottomRight)
    //      .show()
  }

}
