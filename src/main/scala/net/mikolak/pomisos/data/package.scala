package net.mikolak.pomisos

import java.time.Instant

import com.orientechnologies.orient.core.id.ORecordId
import gremlin.scala.{ScalaGraph, id}

import scala.concurrent.duration.Duration

package object data {

  type Id = ORecordId //TODO: switch to IdStandard

  type DB = () => ScalaGraph

  case class Pomodoro(@id id: Id, name: String, completed: Boolean)

  object Pomodoro {

    def apply(name: String): Pomodoro = apply(null, name, completed = false)

  }

  case class TimerPeriod(id: Option[Id], name: String, duration: Duration)

  case class PomodoroRun(endTime: Instant, duration: Duration)
}
