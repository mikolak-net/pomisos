package net.mikolak.pomisos.dependencies

import akka.actor.ActorSystem

trait AkkaModule {

  lazy val actorSystem = ActorSystem("pomisos")

}
