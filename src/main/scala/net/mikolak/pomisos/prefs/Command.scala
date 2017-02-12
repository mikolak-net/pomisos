package net.mikolak.pomisos.prefs

import gremlin.scala.id
import net.mikolak.pomisos.crud.Idable
import net.mikolak.pomisos.data.{IdKey, WithId}
import shapeless.{:+:, CNil}

object Command {

  /*
   * Coproduct for spec types
   */
  type SpecEither = Execution :+: Script :+: CNil

  type FullCommandSpec = (Command, SpecEither)

  val SpecEdge = "specced"

  implicit val specToIds: Idable[FullCommandSpec] = {
    import shapeless._

    object idOfSpec extends Poly1 {
      implicit def caseScript    = at[Script](_.id)
      implicit def caseExecution = at[Execution](_.id)

    }

    new Idable[FullCommandSpec] {
      override def idsOf(spec: FullCommandSpec): Seq[IdKey] = spec match {
        case (command, spec) => Seq(command.id, spec.fold(idOfSpec))
      }
    }
  }

}

case class Command(@id id: IdKey, name: Option[String]) extends WithId

sealed trait CommandSpec extends Product with Serializable with WithId

case class Execution(id: IdKey, cmd: Option[String]) extends CommandSpec

case class Script(id: IdKey, onPomodoro: Option[String], onBreak: Option[String]) extends CommandSpec
