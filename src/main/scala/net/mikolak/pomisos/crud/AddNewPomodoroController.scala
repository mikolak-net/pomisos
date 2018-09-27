package net.mikolak.pomisos.crud

import com.jfoenix.controls.JFXTextField
import net.mikolak.pomisos.utils.Implicits._
import scalafx.beans.property.{IntegerProperty, ObjectProperty}
import scalafx.event.ActionEvent
import scalafx.scene.control.{Button, TextField}
import scalafxml.core.macros.sfxml

import cats.syntax.option._

import scala.util.Try

trait AddNewPomodoro {
  def newPomodoro: ObjectProperty[Option[NewPomodoroRequest]]
}

@sfxml
class AddNewPomodoroController( //TODO: generalize with add new?
                               val addNewArea: TextField,
                               val addCommitmentControl: TextField,
                               val addNewButton: Button)
    extends AddNewPomodoro {

  lazy val newPomodoro = ObjectProperty[Option[NewPomodoroRequest]](None)
  setUpValidation()

  def addItem(event: ActionEvent): Unit =
    for {
      newName ← addNewArea.text.value.toString.some.filter(_.nonEmpty)
    } {
      val commitment = Try(addCommitmentControl.text.value.toInt).getOrElse(1)
      newPomodoro.value = NewPomodoroRequest(newName, commitment).some
      addNewArea.text.value = ""
      addCommitmentControl.text.value = ""
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

case class NewPomodoroRequest(name: String, commitment: Int)
