package net.mikolak.pomisos.quality

import net.mikolak.pomisos.prefs.LengthPreferences

class ConsecutiveQualityAdjuster(consecutiveQuality: ConsecutiveQuality) extends QualityAdjuster {

  import ConsecutiveQualityAdjuster._

  def apply(lengths: LengthPreferences): Option[Int] = {
      consecutiveQuality.predict().map ( quality => {
        val newValue  = Math.max(lengths.pomodoro.toMinutes+(quality-Neutral)*FudgeFactor, lengths.shortBreak.toMinutes).toInt

        newValue
      })
  }

}

object ConsecutiveQualityAdjuster {

  val Neutral = 6

  val FudgeFactor = 2.0

}
