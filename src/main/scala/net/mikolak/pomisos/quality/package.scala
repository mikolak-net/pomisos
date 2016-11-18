package net.mikolak.pomisos

import net.mikolak.pomisos.prefs.LengthPreferences

package object quality {
  sealed trait Quality

  val MinQuality = 0
  val MaxQuality = 10

  type QualityAdjuster = (LengthPreferences) => Option[Int]
}
