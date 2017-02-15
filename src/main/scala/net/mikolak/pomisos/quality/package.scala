package net.mikolak.pomisos

import net.mikolak.pomisos.prefs.LengthPreferences

package object quality {
  sealed trait Quality

  val MinQuality = 0
  val MaxQuality = 10

}

abstract class QualityAdjuster extends ((LengthPreferences) => Option[Int]) {

  val Neutral     = 7
  val FudgeFactor = 1.0

  def predict(): Option[Double]

  def apply(lengths: LengthPreferences): Option[Int] =
    predict().map(quality => {
      val newValue = Math.max(lengths.pomodoro.toMinutes + (quality - Neutral) * FudgeFactor, lengths.shortBreak.toMinutes).toInt

      newValue
    })

}
