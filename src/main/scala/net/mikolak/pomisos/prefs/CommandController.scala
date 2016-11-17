package net.mikolak.pomisos.prefs

import gremlin.scala.ScalaGraph
import net.mikolak.pomisos.crud.{AddNew, AddNewController}

import scalafx.beans.property.ReadOnlyObjectProperty
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{ListView, SelectionMode, ToggleGroup}
import scalafx.scene.control.cell.TextFieldListCell
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafxml.core.macros.{nested, sfxml}
import scalafx.Includes._
import gremlin.scala._

@sfxml
class CommandController(@nested[AddNewController] addNewCmdController: AddNew,
                        commandType: ToggleGroup,
                        val cmdList: ListView[String],
                        db: () => ScalaGraph) {

  lazy val cmdSelected: ReadOnlyObjectProperty[String] = cmdList.getSelectionModel.selectedItemProperty

  commandType.selectedToggle.onChange((_, _, newVal) => println(newVal))

  private def allCmds = db().V.hasLabel[ExecuteCommand].or(_.hasLabel[ScriptCommand])
  val cmds            = ObservableBuffer(allCmds.map(_.toCC[ExecuteCommand].cmd).toList)
  cmdList.setItems(cmds)

  cmds.onChange((buffer, _) => { //YOLO update for now
    allCmds.drop.iterate
    buffer.toList.map(ExecuteCommand.apply).foreach(db().addVertex[ExecuteCommand])
  })

  cmdList.cellFactory = TextFieldListCell.forListView()
  cmdList.editable = false

  def listKeyPressed(event: KeyEvent): Unit =
    if (event.code == KeyCode.Delete && Option(cmdSelected.value).isDefined) {
      cmdList.items.get().remove(cmdList.getSelectionModel.getSelectedIndex)
    }

  cmdList.getSelectionModel.setSelectionMode(SelectionMode.Single)

  addNewCmdController.newName.onChange((obs, _, newItemOpt) =>
    for (newItem <- newItemOpt if !newItem.isEmpty) {
      cmdList.items.get().add(newItem)

      cmdList.getSelectionModel.selectFirst()
  })
}

trait Command {

  def name: Option[String]

}

case class ExecuteCommand(cmd: String) //extends Command

case class ScriptCommand(name: Option[String], onPomodoro: Option[String], onBreak: Option[String]) extends Command
