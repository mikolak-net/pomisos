package net.mikolak.pomisos.crud

import gremlin.scala._
import net.mikolak.pomisos.data._
import net.mikolak.pomisos.utils.Implicits._
import cats.syntax.option._
import scalafx.beans.property.LongProperty

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

  private val LabelRan = "ranAt"
  def addRun(pomId: DbId, run: PomodoroRun) =
    for {
      vPomodoro ← elemWithIdQuery(pomId)
    } {
      db { db =>
        val vRun = db.addVertex(run)
        vPomodoro.addEdge(LabelRan, vRun)
        observablesForRuns.get(pomId.some).foreach(_.value += 1)
      }
    }

  private var observablesForRuns: Map[DbIdKey, LongProperty] = Map.empty

  def getRunsForPomodoro(pomodoro: Pomodoro): LongProperty =
    observablesForRuns.getOrElse(
      pomodoro.id, {
        val value = currentRunsForPomodoro(pomodoro.id)
        println(value)
        val newBuffer = LongProperty(value.getOrElse(0L))

        observablesForRuns += pomodoro.id → newBuffer
        newBuffer
      }
    )

  private def currentRunsForPomodoro(pomodoroId: DbIdKey) =
    pomodoroId.flatMap { id =>
      db(
        _.V
          .hasId(id)
          .outE()
          .hasLabel(LabelRan)
          .count()
          .headOption()
          .map(_.longValue()))
    }
}
