package net.mikolak.pomisos.dependencies

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

trait AkkaModule {

  implicit lazy val actorSystem = ActorSystem("pomisos")

  implicit lazy val materializer = ActorMaterializer()

}
