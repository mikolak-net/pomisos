package net.mikolak.pomisos.utils

import net.mikolak.pomisos.crud.Idable
import net.mikolak.pomisos.data.WithId

import scala.concurrent.duration.Duration
import scala.language.implicitConversions
import scalafx.beans.binding.{Bindings, BooleanBinding, ObjectBinding}
import scalafx.beans.property.ReadOnlyObjectProperty

object Implicits {
  implicit def sDurationToJFxDuration(duration: Duration): javafx.util.Duration = javafx.util.Duration.millis(duration.toMillis)

  implicit class MappableObjectProperty[T](prop: ReadOnlyObjectProperty[T]) {
    def map[X](f: T => X): ObjectBinding[X] = Bindings.createObjectBinding(() => f(prop.value), prop)
  }

  implicit class ObjectToBooleanBinding(bind: ObjectBinding[Boolean]) {

    def toBoolean: BooleanBinding = Bindings.createBooleanBinding(() => bind.value, bind)

  }

  implicit def idableWithId[T <: WithId]: Idable[T] = new Idable[T] {
    override def idsOf(t: T) = Seq(t.id)
  }

}
