package net.mikolak.pomisos.prefs

import gremlin.scala.ScalaGraph

import scala.concurrent.duration.Duration
import scalafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import scalafx.scene.control.{CheckBox, Spinner}
import scalafxml.core.macros.sfxml
import gremlin.scala._
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
                              val preferenceDao: PreferenceDao
                            ) extends  GeneralPrefs {

  def preferences = preferenceDao.get()
  //TODO: shapeless?
  minutesPomodoro.valueFactory = new IntegerSpinnerValueFactory(0, 100, preferences.length.pomodoro.toMinutes.toInt)
  minutesBreakShort.valueFactory = new IntegerSpinnerValueFactory(0, 100, preferences.length.shortBreak.toMinutes.toInt)
  minutesBreakLong.valueFactory = new IntegerSpinnerValueFactory(0, 100, preferences.length.longBreak.toMinutes.toInt)
  numberOfPomodorosUntilLongBreak.valueFactory = new IntegerSpinnerValueFactory(0, 100, preferences.length.pomodorosForLongBreak)
  playTick.selected.value = preferences.playTick

  def commit() = {
    db().V.hasLabel[Preferences].head().updateAs[Preferences](_.copy(LengthPreferences(minutesPomodoro.value.value.toInt minutes,
      minutesBreakShort.value.value.toInt minutes,  minutesBreakLong.value.value.toInt minutes,
      numberOfPomodorosUntilLongBreak.value.value.toInt),
      playTick = playTick.selected.value))
  }


}


case class Preferences(length: LengthPreferences, playTick: Boolean)

case class LengthPreferences(pomodoro: Duration, shortBreak: Duration, longBreak: Duration, pomodorosForLongBreak: Int)

object Preferences {
  def Default = Preferences(LengthPreferences(25 minutes, 5 minutes, 20 minutes, 4), false)

}

class PreferenceDao(db: () => ScalaGraph) {

  def get() = /* Preference.Default.copy(5 seconds, 10 seconds) */ db().V.hasLabel[Preferences].toCC[Preferences].headOption().getOrElse {
    val default = Preferences.Default
    db().addVertex(default)
    default
  }

}