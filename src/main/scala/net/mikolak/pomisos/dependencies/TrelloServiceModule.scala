package net.mikolak.pomisos.dependencies

import akka.actor.Props
import com.softwaremill.macwire.akkasupport._
import com.softwaremill.macwire.wire
import com.softwaremill.tagging._
import net.mikolak.pomisos.prefs.task.{TrelloNetworkService, TrelloSyncActor}

trait TrelloServiceModule { this: AkkaModule with DbModule =>
  lazy val trelloService: TrelloNetworkService = wire[TrelloNetworkService]
  def trelloSyncProps: (TrelloNetworkService) => Props @@ TrelloSyncActor =
    _ => wireProps[TrelloSyncActor].taggedWith[TrelloSyncActor]
}
