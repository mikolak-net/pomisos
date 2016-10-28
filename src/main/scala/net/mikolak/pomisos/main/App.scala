package net.mikolak.pomisos.main

import java.awt.TrayIcon
import java.awt.event.{ActionEvent, ActionListener}
import java.io.IOException

import scalafx.application.Platform
import javafx.scene.Scene

import com.softwaremill.macwire._
import net.mikolak.pomisos.dependencies._

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

  lazy val dependencies = new Dependencies

  val root = FXMLView(resource, new MacWireDependencyResolver(wiredInModule(dependencies)))

  stage = new PrimaryStage() {
    title = "pomisos"
    scene = new Scene(root)
    resizable = false
    icons += dependencies.icon.image
  }

  stage.toFront()

  java.awt.Toolkit.getDefaultToolkit
  val tray = java.awt.SystemTray.getSystemTray
  val trayIcon = new TrayIcon(javax.imageio.ImageIO.read(this.getClass.getResource("/icon_small.png").toURI.toURL),"pomidorosos")
  trayIcon.addActionListener(new ActionListener {
    override def actionPerformed(e: ActionEvent) = Platform.runLater(showStage())
  })
  //TODO: systray issue https://bugs.kde.org/show_bug.cgi?id=362941
//  trayIcon.addMouseListener(new MouseAdapter {
//    override def mouseClicked(e: MouseEvent) = {
//      println("Click")
//      Platform.runLater {
//        stage.show()
//        stage.toFront()
//      }
//    }
//  })
  val menu = new java.awt.PopupMenu()
  val openItem = new java.awt.MenuItem("Show")
  openItem.addActionListener(new ActionListener {
    override def actionPerformed(e: ActionEvent) = Platform.runLater(showStage())
  })
  val exitItem = new java.awt.MenuItem("Exit")
  openItem.addActionListener(new ActionListener {
    override def actionPerformed(e: ActionEvent) = Platform.runLater(doExit())
  })

  menu.add(openItem)
  menu.add(exitItem)
  trayIcon.setPopupMenu(menu)
  tray.add(trayIcon)

  stage.onCloseRequest = (t: WindowEvent) => doExit()

  private def showStage() = {
    stage.show()
    stage.toFront()
  }

  private def doExit() = {
    tray.remove(trayIcon)
    dependencies.actorSystem.terminate()
    Platform.exit()
  }

}


