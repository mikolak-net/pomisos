package net.mikolak.pomisos.utils

import net.mikolak.pomisos.crud.{Idable, MultiDao}
import net.mikolak.pomisos.data.Pomodoro
import net.mikolak.pomisos.prefs.Command.{IdKey, WithId}
import org.apache.tinkerpop.gremlin.structure.T
import shapeless.Lens

import scala.concurrent.duration.Duration
import language.implicitConversions
import scalafx.beans.binding.{Bindings, BooleanBinding, ObjectBinding}
import scalafx.beans.property.{ObjectProperty, ReadOnlyObjectProperty}
import scalafx.collections.ObservableBuffer
import scalafx.collections.ObservableBuffer.{Add, Remove, Update}

object Implicits {
  implicit def sDurationToJFxDuration(duration: Duration): javafx.util.Duration = javafx.util.Duration.millis(duration.toMillis)

  implicit class MappableObjectProperty[T](prop: ReadOnlyObjectProperty[T]) {
    def map[X](f: T => X): ObjectBinding[X] = Bindings.createObjectBinding(() => f(prop.value), prop)
  }

  implicit class ObjectToBooleanBinding(bind: ObjectBinding[Boolean]) {

    def toBoolean: BooleanBinding = Bindings.createBooleanBinding(() => bind.value, bind)

  }

  def propFor[Type <: WithId, FieldType](l: Lens[Type, FieldType], o: Type)(saveFunc: Type => Type): ObjectProperty[FieldType] = {
    var obj = o

    val prop = ObjectProperty.apply(l.get(o))

    prop.onChange((_, _, newVal) => {
      obj = saveFunc(l.set(o)(newVal))
    })

    prop
  }

  def observerFor[T <: Product with Serializable: Idable](
      items: ObservableBuffer[T],
      dao: MultiDao[T]): (ObservableBuffer[_ <: T], Seq[ObservableBuffer.Change[T]]) => Unit =
    (_, rawEvents) => {

      //splice Add and Remove events into collection
      val eventMap = rawEvents.groupBy {
        case Add(i, _)    => i
        case Remove(i, _) => i
      }

      val events = eventMap.toList.flatMap {
        case (index, rowEvents) => {
          if (rowEvents.exists(_.isInstanceOf[Add[T]]) && rowEvents.exists(_.isInstanceOf[Remove[T]])) {
            List(Update(index, index + 1))
          } else {
            rowEvents
          }
        }
      }

      val toId = implicitly[Idable[T]]
      events.collect {
        case Update(from, until) =>
          items.slice(from, until).foreach(dao.save)
        case Remove(_, removed) =>
          val ids = removed.toSeq.flatMap(toId.idsOf)
          dao.remove(ids: _*)
      }
    }

  implicit def idableWithId[T <: WithId]: Idable[T] = new Idable[T] {
    override def idsOf(t: T) = Seq(t.id)
  }

}
