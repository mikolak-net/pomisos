package net.mikolak.pomisos.main

import java.io.IOException

import scalafx.application.Platform
import javafx.scene.Scene

import com.softwaremill.macwire._
import com.softwaremill.tagging._
import net.mikolak.pomisos.dependencies._
import net.mikolak.pomisos.prefs.task.TrelloNetworkService

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.Includes._
import scalafx.stage.WindowEvent
import scalafxml.core.FXMLView
import scalafxml.macwire.MacWireDependencyResolver

object App extends JFXApp {

  val resource = getClass.getResource("main.fxml")
  if (resource == null) {
    throw new IOException("Cannot load resource: main.fxml")
  }

  lazy val dependencies = new Dependencies with AppModule {

    lazy val trelloService = wire[TrelloNetworkService]

    lazy val closeApp = (doExit _).taggedWith[AppCloseFunction]
    lazy val openApp  = (showStage _).taggedWith[AppOpenFunction]

  }

  val root = FXMLView(resource, new MacWireDependencyResolver(wiredInModule(dependencies)))

  stage = new PrimaryStage() {
    title = "pomisos"
    scene = new Scene(root)
    resizable = false
    icons += dependencies.icon.image
  }

  stage.toFront()

  stage.onCloseRequest = (t: WindowEvent) => doExit()

  private def showStage(): Unit = {
    stage.show()
    stage.toFront()
  }

  private def doExit(): Unit = {
    dependencies.actorSystem.terminate()
    Platform.exit()
  }

}
