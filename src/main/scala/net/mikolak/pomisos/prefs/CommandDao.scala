package net.mikolak.pomisos.prefs

import gremlin.scala._
import net.mikolak.pomisos.crud.MultiDao
import net.mikolak.pomisos.data.{DB, IdKey}
import net.mikolak.pomisos.prefs.Command._
import shapeless._

class CommandDao(db: DB) extends MultiDao[FullCommandSpec] {

  override def getAll(): List[FullCommandSpec] = getAllQuery.toList()

  /**
    * Rather hacky conversion function. Will be able to be improved once toCC goes with the planned macro to shapeless switched,
    * no sense using up time for that before.
    */
  private def vertexToSpec(v: Vertex): SpecEither =
    if (v.label() == classOf[Script].getSimpleName) {
      Coproduct[SpecEither](v.toCC[Script])
    } else { //assuming it's Execution for now
      Coproduct[SpecEither](v.toCC[Execution])
    }

  /**
    * Nonparametric conversion - YAGNI (or, more like, "I'm to too tired to experiment right now").
    */
  def convertToOther(spec: FullCommandSpec) = {
    object toOther extends Poly1 {
      implicit def caseExecution = at[Execution](e => Script(None, e.cmd, None))
      implicit def caseScript    = at[Script](s => Execution(None, s.onPomodoro.flatMap(_.split("\n").headOption)))
    }

    val newDetail = spec._2.map(toOther).align[SpecEither]

    val newSpec = (spec._1, newDetail)
    save(newSpec)
  }

  override def save(e: FullCommandSpec): FullCommandSpec =
    e match {
      case (command, spec) =>
        val vCommand =
          command.id.flatMap(id => db().V.hasId(id).headOption().map(_.updateWith(command))).getOrElse(db().addVertex(command))

        (vCommand.toCC[Command], updateSpecDetail(vCommand, spec))
    }

  private def updateSpecDetail(vCommand: Vertex, detail: SpecEither) = {
    //remove previous spec detail, if any
    db().V.hasId(vCommand.id()).outE(SpecEdge).drop().iterate()

    val vSpec = detail match {
      case Inl(execution: Execution) => db().addVertex(execution)
      case Inr(Inl(script: Script))  => db().addVertex(script)
    }

    vCommand.addEdge(SpecEdge, vSpec)

    detail
  }

  override def get(id: IdKey): Option[FullCommandSpec] = getAllQuery.headOption

  private def getAllQuery =
    db().V
      .hasLabel[Command]
      .outE(SpecEdge)
      .map(e => e.outVertex().toCC[Command] -> vertexToSpec(e.inVertex()))

  override def remove(id: IdKey*): Unit = {
    val ids = id.toSet
    getAllQuery.filter { case (command, _) => ids.contains(command.id) }.drop().iterate()
  }

  override def removeAll(): Unit = getAllQuery.drop().iterate()
}
