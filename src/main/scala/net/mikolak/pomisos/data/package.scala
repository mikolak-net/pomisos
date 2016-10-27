package net.mikolak.pomisos

import java.time.Instant

import com.orientechnologies.orient.core.id.ORecordId
import gremlin.scala.id

import scala.concurrent.duration.Duration

package object data {

  type Id = ORecordId

  case class Pomodoro(@id id: Id, name: String, completed: Boolean)


  object Pomodoro {

    def apply(name: String): Pomodoro = apply(null, name, completed = false)

  }

  case class TimerPeriod(id: Option[Id],name: String, duration: Duration)

  case class PomodoroRun(endTime: Instant, duration: Duration)
}
