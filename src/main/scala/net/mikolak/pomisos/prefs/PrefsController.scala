package net.mikolak.pomisos.prefs

import scala.language.postfixOps
import scalafx.beans.property.{BooleanProperty, ObjectProperty}
import scalafx.event.ActionEvent
import scalafx.scene.control.{CheckBox, Spinner}
import scalafx.scene.layout.VBox
import scalafxml.core.macros.{nested, sfxml}

trait PrefsPage {

  def visible: BooleanProperty

  def mode: ObjectProperty[PreferenceMode]

}

@sfxml
class PrefsController(
                       val mainPane: VBox,
                       val prefsView: VBox,
                       @nested[GeneralPrefsController] val prefsViewController: GeneralPrefs,
                       val appsView: VBox
                     ) extends PrefsPage {

  lazy val visible: BooleanProperty = mainPane.visible

  lazy val mode: ObjectProperty[PreferenceMode] = ObjectProperty[PreferenceMode](GeneralPreferences)


  appsView.visible <== mode === AppPreferences
  prefsView.visible <== mode === GeneralPreferences


  def toMain(actionEvent: ActionEvent) = {
    prefsViewController.commit()
    visible.value = false
  }


}




sealed trait PreferenceMode

case object GeneralPreferences extends PreferenceMode
case object AppPreferences extends PreferenceMode