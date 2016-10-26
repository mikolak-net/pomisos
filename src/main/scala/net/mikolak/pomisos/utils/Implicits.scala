package net.mikolak.pomisos.utils

import scala.concurrent.duration.Duration
import language.implicitConversions

object Implicits {
  implicit def sDurationToJFxDuration(duration: Duration): javafx.util.Duration = javafx.util.Duration.millis(duration.toMillis)
}
