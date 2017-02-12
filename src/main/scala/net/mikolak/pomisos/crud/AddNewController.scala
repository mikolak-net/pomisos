package net.mikolak.pomisos.crud

import java.util.function.Predicate

import org.controlsfx.validation.{Severity, ValidationSupport, Validator}

import scalafx.beans.property.ObjectProperty
import scalafx.event.ActionEvent
import scalafx.scene.control.{Button, TextField}
import scalafxml.core.macros.sfxml
import scalafx.Includes._
import scalafx.beans.binding.Bindings

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
    val textEmpty = Bindings.createBooleanBinding(() => addNewArea.text.value.trim().isEmpty, addNewArea.text)
    addNewButton.disable <== textEmpty

    val createItemHandle = addNewArea.onAction.value

    def textValidator =
      Validator.createPredicateValidator(new Predicate[String] {
        override def test(t: String) = !textEmpty.value
      }, "Name must not be empty", Severity.WARNING)

    val validationSupport = new ValidationSupport()
    validationSupport.registerValidator(addNewArea, textValidator)
    validationSupport.initInitialDecoration()

    val onAction = Bindings.createObjectBinding(
      () => {
        val validationResult = Option(validationSupport.validationResultProperty().getValue)
        val allowAdd         = validationResult.exists(_.getWarnings.isEmpty)

        if (allowAdd) createItemHandle else null
      },
      validationSupport.validationResultProperty()
    )

    addNewArea.onAction <== onAction
  }

}
