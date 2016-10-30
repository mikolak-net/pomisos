package net.mikolak.pomisos.dependencies

import com.orientechnologies.orient.core.metadata.schema.OType
import gremlin.scala.{Key, ScalaGraph}
import org.apache.tinkerpop.gremlin.orientdb.{OrientGraph, OrientGraphFactory}
import com.softwaremill.macwire._
import org.apache.commons.configuration.BaseConfiguration
import gremlin.scala._
import net.mikolak.pomisos.data.{Pomodoro, PomodoroRun}
import net.mikolak.pomisos.prefs.{Command, Preferences}
import org.apache.tinkerpop.gremlin.structure.T


trait DbModule {

  lazy val orientGraph = new OrientGraphFactory("plocal:./pomisos").getNoTx

  lazy val scalaDb = wire[ScalaGraph]

}

object DbModule {

  def ensureIndices(g: => OrientGraph): Unit = {

    for {vertexClassObj <- Vertices
         vertexClass = vertexClassObj.getSimpleName
    } {
      val key = Key[String](T.label.getAccessor)
      val config = new BaseConfiguration()
      config.setProperty("keytype", OType.STRING)

      if (!g.getVertexIndexedKeys(vertexClass).contains(key.value)) {
        g.createVertexIndex(key.value, vertexClass, config)
      }
    }

  }

  private val Vertices = List(classOf[Pomodoro], classOf[Preferences], classOf[Command], classOf[PomodoroRun])

}
