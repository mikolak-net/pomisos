package net.mikolak.pomisos.data

import gremlin.scala.{Edge, GremlinScala, ScalaEdge, ScalaGraph, ScalaVertex, Vertex}
import org.apache.tinkerpop.gremlin.orientdb.OrientGraph
import org.apache.tinkerpop.gremlin.structure.Transaction

class ScalaGraphAccess(orientGraph: OrientGraph, scalaGraph: ScalaGraph) {

  import ScalaGraphAccess._

  def apply[Transformed: NotLeakingGraphOutsideOfContext](f: (ScalaGraph) => Transformed): Transformed = synchronized {
    orientGraph.database().activateOnCurrentThread()
    f(scalaGraph)
  }
}

object ScalaGraphAccess {

  import shapeless._

  type NotLeakingGraphOutsideOfContext[T] =
    (|¬|[ScalaGraph] ∧ |¬|[Vertex] ∧ |¬|[Edge] ∧ |¬|[ScalaVertex] ∧
      |¬|[ScalaEdge] ∧ |¬|[Transaction] ∧ |¬|[GremlinScala[_, _]])#λ[T]
}
