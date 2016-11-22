package net.mikolak.pomisos.prefs

import gremlin.scala.ScalaGraph

import scala.concurrent.duration.Duration
import scalafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import scalafx.scene.control.{CheckBox, Spinner}
import scalafxml.core.macros.sfxml
import gremlin.scala._
import net.mikolak.pomisos.crud.Dao

import scala.concurrent.duration._
import language.postfixOps

trait GeneralPrefs {

  def commit(): Unit

}

@sfxml
class GeneralPrefsController(
    db: () => ScalaGraph,
    val minutesPomodoro: Spinner[Integer],
    val minutesBreakShort: Spinner[Integer],
    val minutesBreakLong: Spinner[Integer],
    val numberOfPomodorosUntilLongBreak: Spinner[Integer],
    val playTick: CheckBox,
    val adaptiveEnabled: CheckBox,
    val preferenceDao: PreferenceDao
) extends GeneralPrefs {

  def preferences = preferenceDao.get()
  //TODO: shapeless?
  minutesPomodoro.valueFactory = new IntegerSpinnerValueFactory(0, 100, preferences.length.pomodoro.toMinutes.toInt)
  minutesBreakShort.valueFactory = new IntegerSpinnerValueFactory(0, 100, preferences.length.shortBreak.toMinutes.toInt)
  minutesBreakLong.valueFactory = new IntegerSpinnerValueFactory(0, 100, preferences.length.longBreak.toMinutes.toInt)
  numberOfPomodorosUntilLongBreak.valueFactory = new IntegerSpinnerValueFactory(0, 100, preferences.length.pomodorosForLongBreak)
  playTick.selected.value = preferences.audio.playTick
  adaptiveEnabled.selected.value = preferences.adaptive.enabled

  def commit() =
    preferenceDao.save(
      Preferences(LengthPreferences(minutesPomodoro.value.value.toInt minutes,
                                    minutesBreakShort.value.value.toInt minutes,
                                    minutesBreakLong.value.value.toInt minutes,
                                    numberOfPomodorosUntilLongBreak.value.value.toInt),
                  AudioPreferences(playTick.selected.value),
                  AdaptivePreferences(adaptiveEnabled.selected.value)))

}

case class Preferences(length: LengthPreferences, audio: AudioPreferences, adaptive: AdaptivePreferences)

case class LengthPreferences(pomodoro: Duration, shortBreak: Duration, longBreak: Duration, pomodorosForLongBreak: Int)

case class AudioPreferences(playTick: Boolean)

case class AdaptivePreferences(enabled: Boolean)

object Preferences {
  def Default =
    Preferences(LengthPreferences(25 minutes, 5 minutes, 20 minutes, 4), AudioPreferences(false), AdaptivePreferences(false))

}

class PreferenceDao(db: () => ScalaGraph) extends Dao[Preferences] {

  def get() =
    /* Preference.Default.copy(5 seconds, 10 seconds) */ db().V.hasLabel[Preferences].toCC[Preferences].headOption().getOrElse {
      val default = Preferences.Default
      db().addVertex(default)
      default
    }

  def save(preferences: Preferences) =
    db().V.hasLabel[Preferences].head().updateWith[Preferences](preferences).toCC[Preferences]

  def saveWith(transform: Preferences => Preferences) =
    save(transform(get()))

}
