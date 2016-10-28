package net.mikolak.pomisos.dependencies

import com.orientechnologies.orient.core.metadata.schema.OType
import gremlin.scala.{Key, ScalaGraph}
import org.apache.tinkerpop.gremlin.orientdb.OrientGraphFactory
import com.softwaremill.macwire._
import org.apache.commons.configuration.BaseConfiguration
import gremlin.scala._
import net.mikolak.pomisos.data.Pomodoro

trait DbModule {

  lazy val orientGraph = new OrientGraphFactory("plocal:./pomisos").getNoTx

  lazy val db = {
    lazy val scalaDb = wire[ScalaGraph]

    val key = Key[String]("label")
    val config = new BaseConfiguration()
    config.setProperty("keytype", OType.STRING)

    val vertexClass = "Pomodoro"

    if(orientGraph.getVertexIndexedKeys(vertexClass).contains(key.value)) {
      orientGraph.createVertexIndex(key.value, vertexClass, config)
    } else {
      println("INDEX EXISTS")
      println(scalaDb.V.hasLabel[Pomodoro].explain())
    }

    scalaDb
  }
}
