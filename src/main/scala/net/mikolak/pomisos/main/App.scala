package net.mikolak.pomisos.main

import java.io.File
import java.nio.file.{Files, Path, Paths}

import scalafx.application.Platform
import javafx.scene.Scene

import ch.qos.logback.core.util.StatusPrinter
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

  private def initializeDataPath() = {
    val path = getPathForSystem()
    Files.createDirectories(path)

    val pathString = path.toString + File.separator
    initializeFileLoger(pathString)
    path
  }

  val rootDataPath = initializeDataPath()

  lazy val logger = Logger("main")

  val resource = getClass.getResource("main.fxml")
  if (resource == null) {
    logger.error("Cannot load main UI template file, shutting down!")
    System.exit(1)
  }

  lazy val dependencies = new Dependencies with AppModule {

    lazy val dataPath: Path = rootDataPath

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

  private def getPathForSystem() = {
    val appDirName = "pomisos"

    val homeDir = Paths
      .get(Option(System.getProperty("user.home")).filter(_.trim().nonEmpty).getOrElse(System.getProperty("user.dir")))
      .toAbsolutePath

    val dataBaseDir = System.getProperty("os.name") match {
      case osName if Set("Linux", "Mac OS").exists(osName.startsWith) =>
        Option(System.getenv("XDG_DATA_HOME")).getOrElse(".local/share")
      case _ => ""
    }

    homeDir.resolve(dataBaseDir).resolve(appDirName)
  }

  private def initializeFileLoger(pathString: String) = {
    System.setProperty("pomisos_log_path", pathString)

    //http://stackoverflow.com/a/3810936/724361
    import ch.qos.logback.classic.LoggerContext
    import ch.qos.logback.classic.util.ContextInitializer
    import ch.qos.logback.core.joran.spi.JoranException
    import org.slf4j.LoggerFactory
    val lc = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    val ci = new ContextInitializer(lc)
    lc.reset()
    try {
      ci.autoConfig()
    } catch {
      case e: JoranException =>
        // StatusPrinter will try to log this
        e.printStackTrace()
    }
    StatusPrinter.printInCaseOfErrorsOrWarnings(lc)
  }

}
