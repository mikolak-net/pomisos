package net.mikolak.pomisos

import com.orientechnologies.orient.core.id.ORecordId
import gremlin.scala.id

package object data {

  type Id = ORecordId

  case class Pomodoro(@id id: Id, name: String, completed: Boolean)


  object Pomodoro {

    def apply(name: String): Pomodoro = apply(null, name, completed = false)

  }
}
