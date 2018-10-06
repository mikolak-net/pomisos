package net.mikolak.pomisos

import java.time.Instant

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject
import com.orientechnologies.orient.core.id.ORecordId
import gremlin.scala.{ScalaGraph, id}
import net.mikolak.pomisos.prefs.task.Card
import shapeless.tag.@@

import scala.concurrent.duration.Duration

package object data {

  type DbId = ORecordId

  type DbIdKey = Option[DbId]

  trait WithDbId {
    def id: DbIdKey
  }

  type IdOf[T] = String @@ T

  trait WithGenericId[T] {
    def id: IdOf[T]
  }

  trait GenericIdable[T] {
    def idOf(e: T): Option[IdOf[_]]
  }

  case class Pomodoro(@id id: DbIdKey, name: String, committed: Int, completed: Boolean, cardId: Option[IdOf[Card]])
      extends WithDbId

  object Pomodoro {

    def apply(name: String, committed: Int = 0): Pomodoro =
      apply(None, name, committed, completed = false, None)

  }

  case class TimerPeriod(id: DbIdKey, name: String, duration: Duration)

  case class PomodoroRun(endTime: Instant, duration: Duration)
}
