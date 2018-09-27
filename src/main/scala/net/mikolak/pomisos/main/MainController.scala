package net.mikolak.pomisos.main

import net.mikolak.pomisos.crud.{AddNewPomodoro, AddNewPomodoroController, NewPomodoroRequest}
import net.mikolak.pomisos.graphics.MainIcon
import net.mikolak.pomisos.run.{RunView, RunViewController}
import net.mikolak.pomisos.utils.Notifications
import scalafx.scene.layout.VBox
import scalafxml.core.macros.{nested, sfxml}

import scala.language.{implicitConversions, postfixOps}

@sfxml
class MainController(@nested[AddNewPomodoroController] val newPomodoroController: AddNewPomodoro,
                     @nested[PomodoroTableController] pomodoroTableController: PomodoroTable,
                     @nested[RunViewController] runStatusController: RunView,
                     val management: VBox,
                     val runStatus: VBox,
                     val icon: MainIcon,
                     notifications: Notifications) {

  runStatusController.runningPomodoro <==> pomodoroTableController.pomodoroToRun

  newPomodoroController.newPomodoro.onChange((_, _, newValue) =>
    newValue.foreach { case NewPomodoroRequest(name, commitment) => pomodoroTableController.addItem(name, commitment) })

  management.visible <== !runStatusController.isRunning
  runStatus.visible <== runStatusController.isRunning

}
