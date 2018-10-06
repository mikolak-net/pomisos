package net.mikolak.pomisos.main

import javafx.scene.control.TableCell
import com.jfoenix.controls.JFXButton
import net.mikolak.pomisos.crud.PomodoroDao
import scalafx.scene.control._
import scalafxml.core.macros.sfxml
import scalafx.Includes._
import net.mikolak.pomisos.utils.UiUtils._
import scalafx.beans.property.{BooleanProperty, ObjectProperty, StringProperty}
import net.mikolak.pomisos.data.Pomodoro
import net.mikolak.pomisos.graphics.FontAwesomeGlyphs
import net.mikolak.pomisos.prefs.task.TrelloNetworkService
import net.mikolak.pomisos.ui.CommitmentColorCalculator
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

  def addItem(newName: String, commitment: Int)

}

@sfxml
class PomodoroTableController(
    val pomodoroTable: TableView[Pomodoro],
    val textColumn: TableColumn[Pomodoro, String],
    val commitmentColumn: TableColumn[Pomodoro, String],
    val buttonColumn: TableColumn[Pomodoro, Pomodoro],
    commitmentColorCalculator: CommitmentColorCalculator,
    dao: PomodoroDao,
    glyphs: FontAwesomeGlyphs,
    trelloNetworkService: TrelloNetworkService
) extends PomodoroTable {

  lazy val pomodoroToRun = ObjectProperty[Option[Pomodoro]](None)

  lazy val items = trelloNetworkService.observableList
  pomodoroTable.setItems(items)
  pomodoroTable.getSelectionModel.setSelectionMode(SelectionMode.Single)

  items.onChange(observerFor(dao))

  def addItem(newName: String, commitment: Int) = {
    val newPomodoro = dao.save(Pomodoro(newName, commitment))
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
      def updateComplete(): Unit =
        getPomForCell(cell).foreach { p =>
          val cpl = p.completed
          if (cpl && !cell.styleClass.contains(completeStyle)) {
            cell.styleClass.add(completeStyle)
          } else if (!cpl) {
            cell.styleClass.remove(completeStyle)
          }
        }
      items.onChange(updateComplete())
      cell.item.onChange(updateComplete())
      cell
    })

  commitmentColumn.cellValueFactory = { p =>
    val pomodoroRuns = dao.getRunsForPomodoro(p.value)
    Bindings.createStringBinding(() => {
      val committed = p.value.committed
      s"${pomodoroRuns.value} / $committed"
    }, p.tableView.getItems, pomodoroRuns)
  }

  commitmentColumn.cellFactory = TextFieldTableCell
    .forTableColumn[Pomodoro]()
    .andThen(cell => {

      def updateComplete() = getPomForCell(cell).foreach { p =>
        cell.textFill <== dao.getRunsForPomodoro(p).map(commitmentColorCalculator(p.committed, _).delegate)
      }

      items.onChange(updateComplete())
      cell.item.onChange(updateComplete())
      cell
    })

  textColumn.onEditCommit = (e: CellEditEvent[Pomodoro, String]) => {
    dao.saveWith(_.copy(name = e.newValue))(e.rowValue.id)
  }

  buttonColumn.cellValueFactory = { p =>
    ObjectProperty[Pomodoro](p.value)
  }
  buttonColumn.cellFactory = _ => new ButtonCell(pomodoroToRun, glyphs)

  private def getPomForCell(cell: TableCell[Pomodoro, _]): Option[Pomodoro] = {
    val itemIndex: Int = cell.tableRow.value.indexProperty().intValue()
    (0 until items.length).find(_ == itemIndex).map(_ => items.get(itemIndex))
  }

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
