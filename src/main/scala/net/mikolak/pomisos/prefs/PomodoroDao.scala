package net.mikolak.pomisos.prefs

import gremlin.scala.ScalaGraph
import net.mikolak.pomisos.crud.{Idable, MultiDao}
import net.mikolak.pomisos.data.{DB, IdKey, IdStandard, Pomodoro}
import net.mikolak.pomisos.utils.Implicits._
import gremlin.scala._

class PomodoroDao(db: DB) extends MultiDao[Pomodoro] {

  private def getAllQuery = db().V.hasLabel[Pomodoro]

  private def elemWithIdQuery(id: IdStandard): Option[Vertex] = db().V.hasId(id).headOption()

  override def getAll() = getAllQuery.toCC[Pomodoro].toList()

  override def get(idKey: IdKey): Option[Pomodoro] = idKey.flatMap(id => db().V.hasId(id).toCC[Pomodoro].headOption())

  override def remove(id: IdKey*): Unit = {
    val rawIds = id.toList.flatMap(_.toList)
    db().V.hasId(rawIds.head, rawIds.tail: _*).drop().iterate()
  }

  override def save(e: Pomodoro): Pomodoro =
    e.id.flatMap(elemWithIdQuery).map(_.updateWith(e)).getOrElse(db().addVertex(e)).toCC[Pomodoro]

  override def removeAll(): Unit = getAllQuery.drop().iterate()

}
