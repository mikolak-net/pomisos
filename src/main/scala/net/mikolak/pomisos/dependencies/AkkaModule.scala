package net.mikolak.pomisos.dependencies

import akka.actor.ActorSystem

trait AkkaModule {

  implicit lazy val actorSystem = ActorSystem("pomisos")

}
