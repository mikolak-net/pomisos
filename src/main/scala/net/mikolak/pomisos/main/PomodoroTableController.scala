package net.mikolak.pomisos.main

import javafx.scene.control.TableCell
import com.jfoenix.controls.JFXButton
import scalafx.scene.control._
import scalafxml.core.macros.sfxml
import scalafx.Includes._
import net.mikolak.pomisos.utils.UiUtils._
import scalafx.beans.property.{BooleanProperty, ObjectProperty, StringProperty}
import net.mikolak.pomisos.data.Pomodoro
import net.mikolak.pomisos.graphics.FontAwesomeGlyphs
import net.mikolak.pomisos.prefs.PomodoroDao
import net.mikolak.pomisos.prefs.task.TrelloNetworkService
import org.controlsfx.glyphfont.FontAwesome
import scalafx.beans.binding.Bindings
import scalafx.event.{ActionEvent, Event}
import scalafx.geometry.{Insets, Pos}
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
    glyphs: FontAwesomeGlyphs,
    trelloNetworkService: TrelloNetworkService
) extends PomodoroTable {

  lazy val pomodoroToRun = ObjectProperty[Option[Pomodoro]](None)

  lazy val items = trelloNetworkService.observableList
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

  textColumn.cellValueFactory = { p => Bindings.createStringBinding(() => p.value.name, p.tableView.getItems)
  }
  textColumn.cellFactory = TextFieldTableCell
    .forTableColumn[Pomodoro]()
    .andThen(cell => {
      val completeStyle = "pomodoro-completed-text"
      def updateComplete(): Unit = {
        val itemIndex: Int = cell.tableRow.value.indexProperty().intValue()
        if ((0 until items.length).contains(itemIndex)) {
          val cpl = items.get(itemIndex).completed
          if (cpl && !cell.styleClass.contains(completeStyle)) {
            cell.styleClass.add(completeStyle)
          } else if (!cpl) {
            cell.styleClass.remove(completeStyle)
          }
        }
      }
      items.onChange(updateComplete())
      cell.item.onChange(updateComplete())
      cell
    })

  textColumn.onEditCommit = (e: CellEditEvent[Pomodoro, String]) => {
    dao.saveWith(_.copy(name = e.newValue))(e.rowValue.id)
  }

  buttonColumn.cellValueFactory = { p => ObjectProperty[Pomodoro](p.value)
  }
  buttonColumn.cellFactory = _ => new ButtonCell(pomodoroToRun, glyphs)

}

class ButtonCell(runningPomodoro: ObjectProperty[Option[Pomodoro]], glyphs: FontAwesomeGlyphs)
    extends TableCell[Pomodoro, Pomodoro] {

  val graphic = new HBox() {
    visible = false
    hgrow = Priority.Always
    alignmentInParent = Pos.Center
    alignment = Pos.Center
    spacing = 10.0
  }

  this.onMouseEntered = (x: Event) => graphic.visible = true
  this.onMouseExited = (x: Event) => graphic.visible = false

  this.tableRowProperty.onChange((_, _, newRow) => {
    newRow.onMouseEntered <== this.onMouseEntered
    newRow.onMouseExited <== this.onMouseExited
  })

  private def underlyingButton(glyph: FontAwesome.Glyph) = new JFXButton("", glyphs(glyph))

  val playButton = new Button(underlyingButton(FontAwesome.Glyph.PLAY)) {
    tooltip = "Run this pomodoro"
    onAction = (_: ActionEvent) => runningPomodoro.value = Some(getItem)
  }

  val completeButton = new Button(underlyingButton(FontAwesome.Glyph.CHECK)) {
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
