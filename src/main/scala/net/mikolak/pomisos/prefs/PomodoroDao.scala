package net.mikolak.pomisos.prefs

import gremlin.scala._
import net.mikolak.pomisos.utils.Implicits._
import net.mikolak.pomisos.crud.MultiDao
import net.mikolak.pomisos.data.{DB, DbIdKey, DbId, Pomodoro}

class PomodoroDao(db: DB) extends MultiDao[Pomodoro] {

  private def getAllQuery = db().V.hasLabel[Pomodoro]

  private def elemWithIdQuery(id: DbId): Option[Vertex] = db().V.hasId(id).headOption()

  override def getAll() = getAllQuery.toCC[Pomodoro].toList()

  override def get(idKey: DbIdKey): Option[Pomodoro] = idKey.flatMap(id => db().V.hasId(id).toCC[Pomodoro].headOption())

  override def remove(id: DbIdKey*): Unit = {
    val rawIds = id.toList.flatMap(_.toList)
    if (rawIds.nonEmpty) {
      db().V.hasId(rawIds.head, rawIds.tail: _*).drop().iterate()
    }
  }

  override def save(e: Pomodoro): Pomodoro =
    e.id.flatMap(elemWithIdQuery).map(_.updateWith(e)).getOrElse(db().addVertex(e)).toCC[Pomodoro]

  override def removeAll(): Unit = getAllQuery.drop().iterate()

}
