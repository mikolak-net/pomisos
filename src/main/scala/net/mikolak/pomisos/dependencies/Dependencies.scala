package net.mikolak.pomisos.dependencies

import akka.actor.ActorRef
import com.softwaremill.tagging.@@

class Dependencies
    extends AkkaModule
    with DbModule
    with TimeModule
    with QualityModule
    with UiModule
    with ProcessModule
    with TrelloServiceModule
    with ReporterModule

/**
  * Container class to get around non-working tagged type resolution for ScalaFx DI
  */
case class ActorRefContainer[T](get: ActorRef @@ T)
