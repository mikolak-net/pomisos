package net.mikolak.pomisos.prefs

import net.mikolak.pomisos.crud.{AddNew, AddNewController}
import net.mikolak.pomisos.prefs.Command.{FullCommandSpec, SpecEither, specToIds}
import net.mikolak.pomisos.utils.Implicits._
import net.mikolak.pomisos.utils.UiUtils._
import shapeless.{Coproduct, Inl, Inr, Lens, Poly1, lens}

import scalafx.Includes._
import scalafx.beans.property.ReadOnlyObjectProperty
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.scene.control._
import scalafx.scene.control.cell.TextFieldListCell
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafx.scene.layout.VBox
import scalafx.util.StringConverter
import scalafxml.core.macros.{nested, sfxml}

@sfxml
class CommandController(@nested[AddNewController] addNewCmdController: AddNew,
                        commandType: ToggleGroup,
                        val cmdList: ListView[FullCommandSpec],
                        dao: CommandDao,
                        toggleExecution: RadioButton,
                        toggleScript: RadioButton,
                        adminPaneGeneral: VBox,
                        adminPaneDetail: VBox,
                        adminViewExecution: VBox,
                        executionCommand: TextField,
                        adminViewScript: VBox,
                        scriptStartup: TextArea,
                        scriptStop: TextArea,
                        saveButton: Button) {

  import CommandUi._

  lazy val cmdSelected: ReadOnlyObjectProperty[FullCommandSpec] =
    cmdList.getSelectionModel.selectedItemProperty

  lazy val cmdSelectedIndex = cmdList.getSelectionModel.selectedIndexProperty()

  lazy val cmds = ObservableBuffer(dao.getAll().toList)
  cmdList.setItems(cmds)

  object withConfig extends Poly1 {
    implicit def forScript =
      at[Script](
        _ -> List((lens[Script] >> 'onPomodoro, scriptStartup: TextInputControl),
                  (lens[Script] >> 'onBreak, scriptStop: TextInputControl)))
    implicit def forExecution = at[Execution](_ -> List((lens[Execution] >> 'cmd, executionCommand: TextInputControl)))
  }

  adminPaneGeneral.disable <== cmdSelected.map(_ == null).toBoolean
  adminPaneDetail.visible <== !adminPaneGeneral.disable

  //just need this one
  adminViewExecution.visible <== cmdSelected
    .map(s => s != null && s._2.select[Execution].nonEmpty)
    .toBoolean
  adminViewScript.visible <== !adminViewExecution.visible

  cmdSelected.onChange((_, _, newVal) => {

    newVal match {
      case (_, Inl(_: Execution)) => {

        toggleExecution.selected = true
      }
      case (_, Inr(Inl(_: Script))) => {
        toggleScript.selected = true
      }
      case _ => //fallthrough for null
    }

    loadValues()
  })

  commandType.selectedToggle.onChange((_, _, newVal) => {
    val isExecution    = cmdSelected.value._2.select[Execution].nonEmpty
    val needsSwitching = ((newVal == toggleScript.delegate) && isExecution) || (newVal == toggleExecution.delegate && !isExecution)

    if (needsSwitching) {
      val newSpec       = dao.convertToOther(cmdSelected.value)
      val selectedIndex = cmdList.getSelectionModel.getSelectedIndices.head
      cmds.update(selectedIndex, newSpec)

      //needs to reselect
      cmdList.getSelectionModel.select(selectedIndex)
    }
  })

  cmds.onChange(observerFor[FullCommandSpec](dao))

  cmdList.cellFactory = TextFieldListCell.forListView(new StringConverter[FullCommandSpec] {
    override def fromString(string: String) = ??? //not needed

    override def toString(t: (Command, SpecEither)) = t._1.name.getOrElse("")
  })
  cmdList.editable = false

  def listKeyPressed(event: KeyEvent): Unit =
    if (event.code == KeyCode.Delete && Option(cmdSelected.value).isDefined) {
      cmdList.items.get().remove(cmdList.getSelectionModel.getSelectedIndex)
    }

  cmdList.getSelectionModel.setSelectionMode(SelectionMode.Single)

  addNewCmdController.newName.onChange((obs, _, newItemOpt) =>
    for (newItem <- newItemOpt if !newItem.isEmpty) {
      val newCmd = dao.save(Command(None, Some(newItem)) -> Coproduct[SpecEither](Execution(None, Some(newItem))))

      cmdList.items.get().add(newCmd)

      cmdList.getSelectionModel.selectLast()
  })

  def saveSpec(actionEvent: ActionEvent) = {
    import shapeless._

    val (curCommand, curSpec) = cmdSelected.value

    //TODO: reduce boilerplate 0. zipWithKeys? 1. test with Generic[CommandSpec] 2. Ask on SO
    object applyValues extends Poly1 {
      private def allCases[T <: CommandSpec](arg: SpecWithFields[T]) = arg match {
        case (on, list) =>
          list.foldLeft(on) { case (current, (lensToUse, field)) => lensToUse.set(current)(Option(field.text.value)) }
      }

      implicit def caseScript    = at[SpecWithFields[Script]](allCases)
      implicit def caseExecution = at[SpecWithFields[Execution]](allCases)
    }

    val newSpec = curSpec.map(withConfig).map(applyValues)
    cmds.update(cmdSelectedIndex.intValue(), (curCommand, newSpec))
  }

  private def loadValues(): Unit = {
    import shapeless._
    for ((_, curSpec) <- Option(cmdSelected.value)) {

      //TODO: reduce boilerplate 0. zipWithKeys? 1. test with Generic[CommandSpec] 2. Ask on SO
      object fillText extends Poly1 {
        private def allCases[T <: CommandSpec](arg: SpecWithFields[T]) = arg match {
          case (on, list) =>
            list.foldLeft(on) {
              case (current, (lensToUse, field)) => {
                field.text.value = lensToUse.get(current).getOrElse("")
                current
              }
            }
        }

        implicit def caseScript = at[SpecWithFields[Script]](allCases)

        implicit def caseExecution = at[SpecWithFields[Execution]](allCases)
      }

      curSpec.map(withConfig).map(fillText)
    }
  }
}

object CommandUi {
  type FieldList[T]                     = List[(Lens[T, Option[String]], TextInputControl)]
  type SpecWithFields[T <: CommandSpec] = (T, FieldList[T])
}
