package net.mikolak.pomisos.dependencies

import java.nio.file.Path

import com.orientechnologies.orient.core.metadata.schema.{OClass, OType}
import gremlin.scala.{Key, ScalaGraph, _}
import org.apache.tinkerpop.gremlin.orientdb.{OrientGraph, OrientGraphFactory}
import com.softwaremill.macwire._
import org.apache.commons.configuration.BaseConfiguration
import net.mikolak.pomisos.data.{Pomodoro, PomodoroRun, ScalaGraphAccess}
import net.mikolak.pomisos.prefs._

trait DbModule {

  def dataPath: Path

  private lazy val orientGraph = {
    overrideLoggingToSlf4j()

    //switch to memory:pomisos for debug
    val dbPathString = dataPath.resolve("db").toString

    new OrientGraphFactory(s"plocal:$dbPathString").getNoTx
  }

  private lazy val scalaDb: ScalaGraph = wire[ScalaGraph]

  lazy val graphAccess = wire[ScalaGraphAccess]

  lazy val pomodoroDao = wire[PomodoroDao]

  lazy val preferenceDao = wire[PreferenceDao]

  lazy val commandDao = wire[CommandDao]

  private def overrideLoggingToSlf4j() = {
    import org.slf4j.bridge.SLF4JBridgeHandler
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
    val logger = java.util.logging.Logger.getLogger("com.orientechnologies")
    logger.setLevel(java.util.logging.Level.ALL)
  }
}

object DbModule {

  /**
    * This is unused for the time being, as it does not appear to work reliably.
    */
  def ensureIndices(g: => OrientGraph): Unit =
    for {
      vertexClassObj <- Vertices
      vertexClass = vertexClassObj.getSimpleName
    } {
      import org.apache.tinkerpop.gremlin.structure.T
      val key    = Key[String](T.label.getAccessor)
      val config = new BaseConfiguration()
      config.setProperty("type", OClass.INDEX_TYPE.UNIQUE.name())
      config.setProperty("keytype", OType.STRING)

      if (!g.getVertexIndexedKeys(vertexClass).contains(key.value)) {
        g.createVertexIndex(key.value, vertexClass, config)
      }
    }

  private val Vertices = List(classOf[Pomodoro],
                              classOf[Preferences],
                              classOf[Command],
                              classOf[Command],
                              classOf[Script],
                              classOf[Execution],
                              classOf[PomodoroRun])

}
