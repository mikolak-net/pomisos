package net.mikolak.pomisos.dependencies

import com.softwaremill.macwire.akkasupport._
import com.softwaremill.macwire.wire
import net.mikolak.pomisos.reporting.{DefaultDialogLauncher, DialogLauncher, Reporter, ReportingNotification}

trait ReporterModule { this: AkkaModule =>
  def reporter() = {
    val r = wireActor[Reporter]("reporter")
    actorSystem.eventStream.subscribe(r, classOf[ReportingNotification])
    r
  }

  lazy val dialogLauncher: DialogLauncher = wire[DefaultDialogLauncher]

}
