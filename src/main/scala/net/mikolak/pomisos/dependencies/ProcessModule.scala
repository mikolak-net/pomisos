package net.mikolak.pomisos.dependencies

import com.softwaremill.macwire._
import net.mikolak.pomisos.process.ProcessManager

trait ProcessModule {

  lazy val processManager = wire[ProcessManager]

}
