package net.mikolak.pomisos.dependencies

import com.orientechnologies.orient.core.metadata.schema.{OClass, OType}
import gremlin.scala.{Key, ScalaGraph}
import org.apache.tinkerpop.gremlin.orientdb.{OrientGraph, OrientGraphFactory}
import com.softwaremill.macwire._
import org.apache.commons.configuration.BaseConfiguration
import gremlin.scala._
import net.mikolak.pomisos.data.{Pomodoro, PomodoroRun}
import net.mikolak.pomisos.prefs.{ExecuteCommand, Preferences}
import org.apache.tinkerpop.gremlin.structure.T

trait DbModule {

  lazy val orientGraph = new OrientGraphFactory("plocal:./pomisos").getNoTx

  private lazy val scalaDb: ScalaGraph = wire[ScalaGraph]

  lazy val scalaDbProvider: () => ScalaGraph = () => {
    orientGraph.database().activateOnCurrentThread()
    scalaDb
  }

}

object DbModule {

  def ensureIndices(g: => OrientGraph): Unit = {

    for {
      vertexClassObj <- Vertices
      vertexClass = vertexClassObj.getSimpleName
    } {
      val key    = Key[String](T.label.getAccessor)
      val config = new BaseConfiguration()
      config.setProperty("type", OClass.INDEX_TYPE.UNIQUE.name())
      config.setProperty("keytype", OType.STRING)

      if (!g.getVertexIndexedKeys(vertexClass).contains(key.value)) {
        g.createVertexIndex(key.value, vertexClass, config)
      }
    }

  }

  private val Vertices = List(classOf[Pomodoro], classOf[Preferences], classOf[ExecuteCommand], classOf[PomodoroRun])

}
