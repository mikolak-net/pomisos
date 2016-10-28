package net.mikolak.pomisos.main


import gremlin.scala.ScalaGraph
import net.mikolak.pomisos.crud.{AddNew, AddNewController}
import net.mikolak.pomisos.dependencies.MainIcon
import net.mikolak.pomisos.prefs._
import net.mikolak.pomisos.run.{RunView, RunViewController}
import net.mikolak.pomisos.utils.Notifications

import scala.language.{implicitConversions, postfixOps}
import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.scene.layout.VBox
import scalafxml.core.macros.{nested, sfxml}

@sfxml
class MainController(@nested[AddNewController] val newPomodoroController: AddNew,
                     @nested[PrefsController] val prefsController: PrefsPage,
                     @nested[PomodoroTableController] pomodoroTableController: PomodoroTable,
                     @nested[RunViewController] runStatusController: RunView,
                     val management: VBox,
                     val runStatus: VBox,
                     val stats: VBox,
                     val db: ScalaGraph,
                     val icon: MainIcon,
                     notifications: Notifications) {

  runStatusController.runningPomodoro <==> pomodoroTableController.pomodoroToRun

  newPomodoroController.newName.onChange((_, _, newValue) => newValue.foreach(pomodoroTableController.addItem))

  lazy val prefsVisible = prefsController.visible
  lazy val statsVisible = stats.visible

  management.visible <== !runStatusController.isRunning && !prefsVisible && !statsVisible
  runStatus.visible <== runStatusController.isRunning

  def generalPrefMenu(event: ActionEvent): Unit = {
    prefsController.mode.value = GeneralPreferences
    prefsVisible.value = true
  }

  def appMenu(event: ActionEvent): Unit = {
    prefsController.mode.value = AppPreferences
    prefsVisible.value = true
  }

  def stats30(event: ActionEvent): Unit = {
    statsVisible.value = true
  }

}
