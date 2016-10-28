package net.mikolak.pomisos.main

import java.awt.TrayIcon
import java.awt.event.{ActionEvent, ActionListener}

import com.softwaremill.tagging.@@
import net.mikolak.pomisos.dependencies.{AppCloseFunction, AppOpenFunction}

import scalafx.application.Platform


case class Tray(showApp: (() => Unit) @@ AppOpenFunction, exitApp: (() => Unit) @@ AppCloseFunction) {

  java.awt.Toolkit.getDefaultToolkit
  val tray = java.awt.SystemTray.getSystemTray
  val trayIcon = new TrayIcon(javax.imageio.ImageIO.read(this.getClass.getResource("/icon_small.png").toURI.toURL), "pomidorosos")
  trayIcon.addActionListener(new ActionListener {
    override def actionPerformed(e: ActionEvent) = Platform.runLater(showApp())
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
    override def actionPerformed(e: ActionEvent) = Platform.runLater(showApp())
  })
  val exitItem = new java.awt.MenuItem("Exit")
  openItem.addActionListener(new ActionListener {
    override def actionPerformed(e: ActionEvent) = Platform.runLater(exitApp())
  })

  menu.add(openItem)
  menu.add(exitItem)
  trayIcon.setPopupMenu(menu)
  tray.add(trayIcon)

  def close() = tray.remove(trayIcon)

}
