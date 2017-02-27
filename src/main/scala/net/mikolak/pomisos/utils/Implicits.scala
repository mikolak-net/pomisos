package net.mikolak.pomisos.utils

import net.mikolak.pomisos.crud.DbIdable
import net.mikolak.pomisos.data._

import scala.concurrent.duration.Duration
import scala.language.implicitConversions
import scalafx.beans.binding.{Bindings, BooleanBinding, ObjectBinding}
import scalafx.beans.property.ReadOnlyObjectProperty
import scalafx.beans.value.ObservableValue

object Implicits {
  implicit def sDurationToJFxDuration(duration: Duration): javafx.util.Duration = javafx.util.Duration.millis(duration.toMillis)

  implicit class MappableObjectBinding[T <: AnyRef](prop: ObservableValue[T, _]) {
    def map[X](f: T => X): ObjectBinding[X] = Bindings.createObjectBinding(() => f(prop.value), prop)
  }

  implicit class ObjectToBooleanBinding(bind: ObjectBinding[Boolean]) {

    def toBoolean: BooleanBinding = Bindings.createBooleanBinding(() => bind.value, bind)

  }

  implicit def dbIdableWithId[T <: WithDbId]: DbIdable[T] = new DbIdable[T] {
    override def idsOf(t: T) = Seq(t.id)
  }

  implicit def genericIdableWithId[T <: WithGenericId[T]]: GenericIdable[T] = new GenericIdable[T] {
    override def idOf(e: T) = Some(e.id)
  }

  implicit def genericIdableForPomodoro: GenericIdable[Pomodoro] = new GenericIdable[Pomodoro] {
    override def idOf(e: Pomodoro): Option[IdOf[_]] = e.cardId
  }

}
