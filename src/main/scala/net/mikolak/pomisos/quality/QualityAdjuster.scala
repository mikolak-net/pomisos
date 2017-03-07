package net.mikolak.pomisos.quality

import net.mikolak.pomisos.prefs.LengthPreferences

abstract class QualityAdjuster extends ((LengthPreferences) => Option[Int]) {

  val Neutral     = 7
  val FudgeFactor = 1.0

  final def apply(lengths: LengthPreferences): Option[Int] =
    predictWithData(getData()).map(quality => {
      val newValue = Math.max(lengths.pomodoro.toMinutes + (quality - Neutral) * FudgeFactor, lengths.shortBreak.toMinutes).toInt

      newValue
    })

  protected[quality] def predictWithData(lastQualities: List[PomodoroQuality]): Option[Double]

  protected[quality] def getData(): List[PomodoroQuality]

}
