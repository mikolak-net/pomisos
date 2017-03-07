package net.mikolak.pomisos.dependencies

import java.time.{Clock, ZoneId}

trait TimeModule {

  lazy val timeZone: ZoneId = ZoneId.systemDefault()

  lazy val clock: Clock = Clock.system(timeZone)

}
