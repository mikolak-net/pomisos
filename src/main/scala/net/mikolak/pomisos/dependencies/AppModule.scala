package net.mikolak.pomisos.dependencies

import net.mikolak.pomisos.main.Tray
import com.softwaremill.macwire._

trait AppModule extends AppLifecycleModule {

  lazy val tray = wire[Tray]

}
