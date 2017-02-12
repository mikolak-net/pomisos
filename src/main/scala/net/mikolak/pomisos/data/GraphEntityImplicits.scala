package net.mikolak.pomisos.data

import net.mikolak.pomisos.prefs.Command.WithId
import gremlin.scala._

object GraphEntityImplicits {

  implicit class GraphBackedEntity[T <: Product with Serializable with WithId](e: T) {

    def vertex(implicit db: () => ScalaGraph): Option[ScalaVertex] =
      e.id.flatMap(id => db().V.hasId(id).headOption()).map(_.asScala)

    def edge(implicit db: () => ScalaGraph): Option[ScalaEdge] = e.id.flatMap(id => db().E.hasId(id).headOption()).map(_.asScala)

  }

}
