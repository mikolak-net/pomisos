package net.mikolak.pomisos.crud


import scalafx.beans.property.ObjectProperty
import scalafx.event.ActionEvent
import scalafx.scene.control.{Button, TextField}
import scalafxml.core.macros.sfxml
import scalafx.Includes._
import scalafx.scene.input.{KeyCode, KeyCombination}


trait AddNew {
  def newName: ObjectProperty[Option[String]]
}

@sfxml
class AddNewController(
                        val addNewArea: TextField
                      ) extends AddNew {

  lazy val newName = ObjectProperty[Option[String]](None)

  def addItem(event: ActionEvent): Unit = {
    newName.value = Some(addNewArea.text.value.toString).filter(_.nonEmpty)
    addNewArea.text.value = ""
  }

}
