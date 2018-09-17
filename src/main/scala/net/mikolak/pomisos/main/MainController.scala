package net.mikolak.pomisos.main

import net.mikolak.pomisos.crud.{AddNew, AddNewController}
import net.mikolak.pomisos.graphics.MainIcon
import net.mikolak.pomisos.run.{RunView, RunViewController}
import net.mikolak.pomisos.utils.Notifications

import scala.language.{implicitConversions, postfixOps}
import scalafx.scene.layout.VBox
import scalafxml.core.macros.{nested, sfxml}

@sfxml
class MainController(@nested[AddNewController] val newPomodoroController: AddNew,
                     @nested[PomodoroTableController] pomodoroTableController: PomodoroTable,
                     @nested[RunViewController] runStatusController: RunView,
                     val management: VBox,
                     val runStatus: VBox,
                     val icon: MainIcon,
                     notifications: Notifications) {

  runStatusController.runningPomodoro <==> pomodoroTableController.pomodoroToRun

  newPomodoroController.newName.onChange((_, _, newValue) => newValue.foreach(pomodoroTableController.addItem))

  management.visible <== !runStatusController.isRunning
  runStatus.visible <== runStatusController.isRunning

}
