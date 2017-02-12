package net.mikolak.pomisos.main

import javafx.scene.control.TableCell

import scalafx.scene.control._
import scalafxml.core.macros.sfxml
import scalafx.Includes._
import scalafx.beans.property.{BooleanProperty, ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import net.mikolak.pomisos.data.Pomodoro
import net.mikolak.pomisos.graphics.FontAwesomeGlyphs
import net.mikolak.pomisos.prefs.PomodoroDao
import org.controlsfx.glyphfont.FontAwesome

import scalafx.beans.binding.Bindings
import scalafx.event.{ActionEvent, Event}
import scalafx.geometry.Pos
import scalafx.scene.control.TableColumn.CellEditEvent
import scalafx.scene.control.cell.TextFieldTableCell
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafx.scene.layout.{HBox, Priority}
import net.mikolak.pomisos.utils.Implicits._

trait PomodoroTable {

  def pomodoroToRun: ObjectProperty[Option[Pomodoro]]

  def addItem(newName: String)

}

@sfxml
class PomodoroTableController(
    val pomodoroTable: TableView[Pomodoro],
    val textColumn: TableColumn[Pomodoro, String],
    val buttonColumn: TableColumn[Pomodoro, Pomodoro],
    dao: PomodoroDao,
    glyphs: FontAwesomeGlyphs
) extends PomodoroTable {

  lazy val pomodoroToRun = ObjectProperty[Option[Pomodoro]](None)

  lazy val items = ObservableBuffer[Pomodoro](dao.getAll())
  pomodoroTable.setItems(items)
  pomodoroTable.getSelectionModel.setSelectionMode(SelectionMode.Single)

  items.onChange(observerFor(dao))

  def addItem(newName: String) = {
    val newPomodoro = dao.save(Pomodoro(newName))
    items.add(newPomodoro)
  }

  lazy val selectedIndex = pomodoroTable.getSelectionModel.selectedIndexProperty()

  pomodoroTable.onKeyPressed = (k: KeyEvent) => {
    Option(selectedIndex.value).filter(_ => k.code == KeyCode.Delete).foreach(i => items.remove(i.toInt))
  }

  textColumn.cellValueFactory = { p =>
    Bindings.createStringBinding(() => p.value.name, p.tableView.getItems)
  }
  textColumn.cellFactory = TextFieldTableCell
    .forTableColumn[Pomodoro]()
    .andThen(cell => {
      val completeStyle = "pomodoro-completed-text"
      def updateComplete(): Unit = {
        val itemIndex: Int = cell.tableRow.value.indexProperty().intValue()
        if ((0 until items.length).contains(itemIndex)) {
          val cpl = items.get(itemIndex).completed
          if (cpl) {
            cell.styleClass.add(completeStyle)
          } else {
            cell.styleClass.remove(completeStyle)
          }
        }
      }
      //cell.tableRow.onChange(updateComplete())
      items.onChange(updateComplete())
      cell.item.onChange(updateComplete())
      cell
    }) //TODO: this should parse w/o .value and be editable

  textColumn.onEditCommit = (e: CellEditEvent[Pomodoro, String]) => {
    dao.saveWith(_.copy(name = e.newValue))(e.rowValue.id)
  }

  buttonColumn.cellValueFactory = { p =>
    ObjectProperty[Pomodoro](p.value)
  }
  buttonColumn.cellFactory = _ => new ButtonCell(pomodoroToRun, glyphs)

}

class ButtonCell(runningPomodoro: ObjectProperty[Option[Pomodoro]], glyphs: FontAwesomeGlyphs)
    extends TableCell[Pomodoro, Pomodoro] {

  val graphic = new HBox() {
    visible = false
    hgrow = Priority.Always
    alignmentInParent = Pos.Center
  }

  this.onMouseEntered = (x: Event) => graphic.visible = true
  this.onMouseExited = (x: Event) => graphic.visible = false

  this.tableRowProperty.onChange((_, _, newRow) => {
    newRow.onMouseEntered <== this.onMouseEntered
    newRow.onMouseExited <== this.onMouseExited
  })

  val playButton = new Button("", glyphs(FontAwesome.Glyph.PLAY)) {
    tooltip = "Run this pomodoro"
    onAction = (_: ActionEvent) => runningPomodoro.value = Some(getItem)
  }

  val completeButton = new Button("", glyphs(FontAwesome.Glyph.CHECK)) {
    tooltip = "Mark this pomodoro as completed"
    onAction = (_: ActionEvent) => getTableColumn.getTableView.getItems.update(getIndex, getItem.copy(completed = true))
  }

  graphic.children = List(playButton, completeButton)

  override def updateItem(t: Pomodoro, b: Boolean) = {
    super.updateItem(t, b)
    if (!b && t != null && !t.completed) {
      setGraphic(graphic)
    } else {
      setGraphic(null)
    }
  }
}
