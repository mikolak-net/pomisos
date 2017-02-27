package net.mikolak.pomisos.utils

import net.mikolak.pomisos.crud.{DbIdable, MultiDao}
import net.mikolak.pomisos.data.WithDbId
import shapeless.Lens

import scalafx.beans.property.ObjectProperty
import scalafx.collections.ObservableBuffer
import scalafx.collections.ObservableBuffer.{Add, Remove, Update}

object UiUtils {

  def propFor[Type <: WithDbId, FieldType](l: Lens[Type, FieldType], o: Type)(
      saveFunc: Type => Type): ObjectProperty[FieldType] = {
    var obj = o

    val prop = ObjectProperty.apply(l.get(o))

    prop.onChange((_, _, newVal) => {
      obj = saveFunc(l.set(o)(newVal))
    })

    prop
  }

  def observerFor[T <: Product with Serializable: DbIdable](
      dao: MultiDao[T]): (ObservableBuffer[_ <: T], Seq[ObservableBuffer.Change[T]]) => Unit =
    (items, rawEvents) => {

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

      val toId = implicitly[DbIdable[T]]
      events.collect {
        case Update(from, until) =>
          items.slice(from, until).foreach(dao.save)
        case Remove(_, removed) =>
          val ids = removed.toSeq.flatMap(toId.idsOf)
          dao.remove(ids: _*)
      }
    }

}
