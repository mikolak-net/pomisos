package net.mikolak.pomisos.prefs

import gremlin.scala._
import net.mikolak.pomisos.utils.Implicits._
import net.mikolak.pomisos.crud.MultiDao
import net.mikolak.pomisos.data.{DbId, DbIdKey, Pomodoro, ScalaGraphAccess}

class PomodoroDao(db: ScalaGraphAccess) extends MultiDao[Pomodoro] {

  private val getAllQuery = (db: ScalaGraph) => db.V.hasLabel[Pomodoro]

  private def elemWithIdQuery(id: DbId): Option[Vertex] = db(_.V.hasId(id).headOption())

  override def getAll() = db(getAllQuery.andThen(_.toCC[Pomodoro].toList()))

  override def get(idKey: DbIdKey): Option[Pomodoro] = idKey.flatMap(id => db(_.V.hasId(id).toCC[Pomodoro].headOption()))

  override def remove(id: DbIdKey*): Unit = {
    val rawIds = id.toList.flatMap(_.toList)
    if (rawIds.nonEmpty) {
      db[Unit](_.V.hasId(rawIds.head, rawIds.tail: _*).drop().iterate())
    }
  }

  override def save(e: Pomodoro): Pomodoro =
    e.id.flatMap(elemWithIdQuery).map(_.updateWith(e)).getOrElse(db(_.addVertex(e))).toCC[Pomodoro]

  override def removeAll(): Unit = db[Unit](getAllQuery.andThen(_.drop().iterate()))

}
