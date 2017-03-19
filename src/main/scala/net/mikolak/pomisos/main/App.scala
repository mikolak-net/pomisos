package net.mikolak.pomisos.main

import java.io.IOException

import scalafx.application.Platform
import javafx.scene.Scene

import com.softwaremill.macwire._
import com.softwaremill.tagging._
import com.typesafe.scalalogging.Logger
import net.mikolak.pomisos.dependencies._

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.Includes._
import scalafx.stage.WindowEvent
import scalafxml.core.FXMLView
import scalafxml.macwire.MacWireDependencyResolver

object App extends JFXApp {

  val logger = Logger("main")

  val resource = getClass.getResource("main.fxml")
  if (resource == null) {
    logger.error("Cannot load main UI template file, shutting down!")
    System.exit(1)
  }

  lazy val dependencies = new Dependencies with AppModule {

    lazy val closeApp = (doExit _).taggedWith[AppCloseFunction]
    lazy val openApp  = (doInit _).taggedWith[AppOpenFunction]

  }

  val root = FXMLView(resource, new MacWireDependencyResolver(wiredInModule(dependencies)))

  stage = new PrimaryStage() {
    title = "pomisos"
    scene = new Scene(root)
    resizable = false
    icons += dependencies.icon.image
  }

  stage.onCloseRequest = (t: WindowEvent) => dependencies.closeApp()

  dependencies.openApp()

  private def doInit(): Unit = {
    initReporter()
    showStage()
    logger.info("Pomisos is initialized")
  }

  private def initReporter(): Unit =
    dependencies.reporter() //forces resolution/instantiation

  private def showStage(): Unit = {
    stage.show()
    stage.toFront()
  }

  private def doExit(): Unit = {
    logger.info("Pomisos is shutting down")
    dependencies.actorSystem.terminate()
    Platform.exit()
  }

}
