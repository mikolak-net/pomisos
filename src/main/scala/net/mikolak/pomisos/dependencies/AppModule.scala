package net.mikolak.pomisos.dependencies

import com.softwaremill.macwire.wire
import net.mikolak.pomisos.main.Tray

trait AppModule extends AppLifecycleModule {

  lazy val tray = wire[Tray]

}
