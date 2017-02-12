package net.mikolak.pomisos

import java.time.Instant

import com.orientechnologies.orient.core.id.ORecordId
import gremlin.scala.{ScalaGraph, id}

import scala.concurrent.duration.Duration

package object data {

  type DB = () => ScalaGraph

  type IdStandard = ORecordId

  type IdKey = Option[IdStandard]

  trait WithId {
    def id: IdKey
  }

  case class Pomodoro(@id id: IdKey, name: String, completed: Boolean) extends WithId

  object Pomodoro {

    def apply(name: String): Pomodoro = apply(None, name, completed = false)

  }

  case class TimerPeriod(id: IdKey, name: String, duration: Duration)

  case class PomodoroRun(endTime: Instant, duration: Duration)
}
