package net.mikolak.pomisos.dependencies

import gremlin.scala.ScalaGraph
import org.apache.tinkerpop.gremlin.orientdb.OrientGraphFactory
import com.softwaremill.macwire._

trait DbModule {

  lazy val orientGraph = new OrientGraphFactory("plocal:./pomisos").getNoTx

  lazy val db = wire[ScalaGraph]
}
