package net.mikolak.pomisos.data

import gremlin.scala._

object GraphEntityImplicits {

  implicit class GraphBackedEntity[T <: Product with Serializable with WithDbId](e: T) {

    def vertex(implicit db: DB): Option[ScalaVertex] =
      e.id.flatMap(id => db().V.hasId(id).headOption()).map(_.asScala)

    def edge(implicit db: DB): Option[ScalaEdge] = e.id.flatMap(id => db().E.hasId(id).headOption()).map(_.asScala)

  }

}
