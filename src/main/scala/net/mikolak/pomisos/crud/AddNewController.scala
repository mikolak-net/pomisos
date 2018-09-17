package net.mikolak.pomisos.crud

import com.jfoenix.controls.JFXTextField
import net.mikolak.pomisos.utils.Implicits._
import scalafx.beans.property.ObjectProperty
import scalafx.event.ActionEvent
import scalafx.scene.control.{Button, TextField}
import scalafxml.core.macros.sfxml

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

  private def setUpValidation(): Unit = {
    val textEmpty = addNewArea.text.mapToBoolean(t => t == null || t.trim().isEmpty)
    addNewButton.disable <== textEmpty

    val jfxAddNewArea = addNewArea.delegate.asInstanceOf[JFXTextField] //required in lieu of custom wrapper
    jfxAddNewArea
      .textProperty()
      .addListener((_, oldVal, newVal) => {
        if (oldVal != newVal) jfxAddNewArea.validate
      })

  }

}
