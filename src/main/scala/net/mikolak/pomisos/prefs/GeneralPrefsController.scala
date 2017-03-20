package net.mikolak.pomisos.prefs

import gremlin.scala._
import net.mikolak.pomisos.crud.{AddNew, SingletonDao}
import net.mikolak.pomisos.data.{IdOf, ScalaGraphAccess}
import net.mikolak.pomisos.prefs.ColumnType.ColumnType
import net.mikolak.pomisos.prefs.NotifySound.NotificationSound
import net.mikolak.pomisos.prefs.task.{Board, CardList}

import scala.concurrent.duration._
import scala.language.postfixOps
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import scalafx.scene.control.{CheckBox, ComboBox, Spinner}
import scalafx.util.StringConverter
import scalafxml.core.macros.{nested, sfxml}
import scalafx.Includes._

trait GeneralPrefs {

  def commit(): Unit

}

@sfxml
class GeneralPrefsController(
    @nested[TrelloPrefsController] trelloPrefsController: TrelloPrefs,
    val minutesPomodoro: Spinner[Integer],
    val minutesBreakShort: Spinner[Integer],
    val minutesBreakLong: Spinner[Integer],
    val numberOfPomodorosUntilLongBreak: Spinner[Integer],
    val playTick: CheckBox,
    val notificationSound: ComboBox[Option[NotificationSound]],
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
  notificationSound.converter = StringConverter.toStringConverter(_.map(_.toString).getOrElse("NONE"))
  private val soundValues = List[Option[NotificationSound]](None) ++ NotifySound.values.toList
    .map(e => Option[NotificationSound](e))
  notificationSound.items = ObservableBuffer(soundValues)
  notificationSound.getSelectionModel.select(preferences.audio.notificationSound)
  adaptiveEnabled.selected.value = preferences.adaptive.enabled

  def commit() =
    preferenceDao.save(
      Preferences(
        LengthPreferences(
          minutesPomodoro.value.value.toInt minutes,
          minutesBreakShort.value.value.toInt minutes,
          minutesBreakLong.value.value.toInt minutes,
          numberOfPomodorosUntilLongBreak.value.value.toInt
        ),
        AudioPreferences(playTick.selected.value, notificationSound.getSelectionModel.getSelectedItem),
        AdaptivePreferences(adaptiveEnabled.selected.value),
        trelloPrefsController.prefs
      )
    )

}

case class Preferences(length: LengthPreferences,
                       audio: AudioPreferences,
                       adaptive: AdaptivePreferences,
                       trello: Option[TrelloPreferences])

case class LengthPreferences(pomodoro: Duration, shortBreak: Duration, longBreak: Duration, pomodorosForLongBreak: Int)

case class AudioPreferences(playTick: Boolean, notificationSound: Option[NotificationSound])

case class AdaptivePreferences(enabled: Boolean)

object NotifySound extends Enumeration {
  type NotificationSound = Value

  val KetchupBottle = Value("Ketchup Bottle")
  val AmbientChime  = Value("Ambient Chime")
  val Chime         = Value("Chime")
  val Synth         = Value("Synth")
}

case class TrelloPreferences(authToken: Option[String], board: Option[IdOf[Board]], columns: Map[ColumnType, IdOf[CardList]])

object ColumnType extends Enumeration {
  type ColumnType = Value

  val ToDo, Doing, Done = Value
}

object Preferences {
  def Default =
    Preferences(
      LengthPreferences(25 minutes, 5 minutes, 20 minutes, 4),
      AudioPreferences(playTick = false, Some(NotifySound.KetchupBottle)),
      AdaptivePreferences(enabled = false),
      None
    )

}

class PreferenceDao(db: ScalaGraphAccess) extends SingletonDao[Preferences] {

  def get() =
    db(
      db =>
        db.V
          .hasLabel[Preferences]
          .toCC[Preferences]
          .headOption()
          .getOrElse {
            val default = Preferences.Default
            db.addVertex(default)
            default
        })

  def save(preferences: Preferences) =
    db(
      _.V
        .hasLabel[Preferences]
        .head()
        .updateWith[Preferences](preferences)
        .toCC[Preferences])

  override def removeAll(): Unit = db[Unit](_.V.hasLabel[Preferences].drop.iterate())
}
