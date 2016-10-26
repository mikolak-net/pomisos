package net.mikolak.pomisos.prefs

import javafx.collections.FXCollections

import gremlin.scala.ScalaGraph
import net.mikolak.pomisos.crud.{AddNew, AddNewController}

import scala.concurrent.duration._
import language.postfixOps
import scalafx.beans.property.{BooleanProperty, ObjectProperty, ReadOnlyObjectProperty}
import scalafx.event.ActionEvent
import scalafx.scene.layout.{GridPane, VBox}
import scalafxml.core.macros.{nested, sfxml}
import gremlin.scala._

import scalafx.Includes._
import scala.concurrent.duration.Duration
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{CheckBox, ListView, SelectionMode, Spinner}
import scalafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import scalafx.scene.control.cell.TextFieldListCell
import scalafx.scene.input.{KeyCode, KeyEvent}

trait PrefsPage {

  def visible: BooleanProperty

  def mode: ObjectProperty[PreferenceMode]

}

@sfxml
class PrefsController(
                       val mainPane: VBox,
                       val prefsView: VBox,
                       val appsView: VBox,
                       @nested[AddNewController] addNewCmdController: AddNew,
                       val cmdList: ListView[String],
                       db: ScalaGraph,
                       val minutesPomodoro: Spinner[Integer],
                       val minutesBreakShort: Spinner[Integer],
                       val minutesBreakLong: Spinner[Integer],
                       val numberOfPomodorosUntilLongBreak: Spinner[Integer],
                       val playTick: CheckBox
                     ) extends PrefsPage {

  lazy val visible: BooleanProperty = mainPane.visible

  lazy val mode: ObjectProperty[PreferenceMode] = ObjectProperty[PreferenceMode](GeneralPreferences)

  lazy val cmdSelected: ReadOnlyObjectProperty[String] = cmdList.getSelectionModel.selectedItemProperty

  appsView.visible <== mode === AppPreferences
  prefsView.visible <== mode === GeneralPreferences

  def preferences = Preferences.current(db)
  //TODO: shapeless?
  minutesPomodoro.valueFactory = new IntegerSpinnerValueFactory(0, 100, preferences.length.pomodoro.toMinutes.toInt)
  minutesBreakShort.valueFactory = new IntegerSpinnerValueFactory(0, 100, preferences.length.shortBreak.toMinutes.toInt)
  minutesBreakLong.valueFactory = new IntegerSpinnerValueFactory(0, 100, preferences.length.longBreak.toMinutes.toInt)
  numberOfPomodorosUntilLongBreak.valueFactory = new IntegerSpinnerValueFactory(0, 100, preferences.length.pomodorosForLongBreak)
  playTick.selected.value = preferences.playTick

  private def allCmds = db.V.hasLabel[Command]
  val cmds = ObservableBuffer(allCmds.map(_.toCC[Command].cmd).toList)
  cmdList.setItems(cmds)

  cmds.onChange((buffer, _) => { //YOLO update for now
    allCmds.drop.iterate
    buffer.toList.map(Command.apply).foreach(db.addVertex[Command])
  })


  cmdList.cellFactory = TextFieldListCell.forListView()
  cmdList.editable = true

  def listKeyPressed(event: KeyEvent): Unit = {
    if(event.code == KeyCode.Delete && Option(cmdSelected.value).isDefined) {
      cmdList.items.get().remove(cmdList.getSelectionModel.getSelectedIndex)
    }
  }

  cmdList.getSelectionModel.setSelectionMode(SelectionMode.Single)

  addNewCmdController.newName.onChange( (obs, _, newItemOpt) => for(newItem <- newItemOpt if !newItem.isEmpty) {
    cmdList.items.get().add(newItem)

    cmdList.getSelectionModel.selectFirst()
  })

  def toMain(actionEvent: ActionEvent) = {
    db.V.hasLabel[Preferences].head().updateAs[Preferences](_.copy(LengthPreferences(minutesPomodoro.value.value.toInt minutes,
      minutesBreakShort.value.value.toInt minutes,  minutesBreakLong.value.value.toInt minutes,
      numberOfPomodorosUntilLongBreak.value.value.toInt),
      playTick = playTick.selected.value))
    visible.value = false
  }


}

case class Command(cmd: String)

case class Preferences(length: LengthPreferences, playTick: Boolean)

case class LengthPreferences(pomodoro: Duration, shortBreak: Duration, longBreak: Duration, pomodorosForLongBreak: Int)

object Preferences {
  def Default = Preferences(LengthPreferences(25 minutes, 5 minutes, 20 minutes, 4), false)

  def current(db: ScalaGraph) = /* Preference.Default.copy(5 seconds, 10 seconds) */ db.V.hasLabel[Preferences].toCC[Preferences].headOption().getOrElse {
    val default = Preferences.Default
    db.addVertex(default)
    default
  }
}

sealed trait PreferenceMode

case object GeneralPreferences extends PreferenceMode
case object AppPreferences extends PreferenceMode