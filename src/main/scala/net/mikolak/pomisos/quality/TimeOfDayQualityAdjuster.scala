package net.mikolak.pomisos.quality

import net.mikolak.pomisos.prefs.LengthPreferences

class TimeOfDayQualityAdjuster(qual: TimeOfDayQuality) extends QualityAdjuster {
  import TimeOfDayQualityAdjuster._

  def apply(lengths: LengthPreferences): Option[Int] = {
    qual.predict().map ( quality => {
      val newValue  = Math.max(lengths.pomodoro.toMinutes+(quality-Neutral)*FudgeFactor, lengths.shortBreak.toMinutes).toInt

      newValue
    })
  }
}

object TimeOfDayQualityAdjuster {

  val Neutral = 7
  val FudgeFactor = 1.0

}
