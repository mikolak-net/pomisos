package net.mikolak.pomisos.crud

import java.util.function.Predicate

import org.controlsfx.validation.{Severity, ValidationResult, ValidationSupport, Validator}

import scalafx.beans.property.{ObjectProperty, ReadOnlyObjectProperty}
import scalafx.event.ActionEvent
import scalafx.scene.control.{Button, TextField}
import scalafxml.core.macros.sfxml
import scalafx.Includes._
import scalafx.beans.binding.Bindings
import net.mikolak.pomisos.utils.Implicits._

trait AddNew {
  def newName: ObjectProperty[Option[String]]
}

@sfxml
class AddNewController(
    val addNewArea: TextField,
    val addNewButton: Button
) extends AddNew {

  lazy val newName = ObjectProperty[Option[String]](None)
  setUpValidation()

  def addItem(event: ActionEvent): Unit = {
    newName.value = Some(addNewArea.text.value.toString).filter(_.nonEmpty)
    addNewArea.text.value = ""
  }

  private def setUpValidation() = {
    val textEmpty = addNewArea.text.mapToBoolean(_.trim().isEmpty)
    addNewButton.disable <== textEmpty

    val createItemHandle = addNewArea.onAction.value

    def textValidator =
      Validator.createPredicateValidator(new Predicate[String] {
        override def test(t: String) = !textEmpty.value
      }, "Name must not be empty", Severity.WARNING)

    val validationSupport = new ValidationSupport()
    validationSupport.registerValidator(addNewArea, textValidator)
    validationSupport.initInitialDecoration()

    val validationResult: ReadOnlyObjectProperty[ValidationResult] = validationSupport.validationResultProperty()

    addNewArea.onAction <== validationResult.mapNullable(vR => if (vR.exists(_.getWarnings.isEmpty)) createItemHandle else null)
  }

}
