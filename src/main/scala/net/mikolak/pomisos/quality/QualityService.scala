package net.mikolak.pomisos.quality

import java.time.Instant

import gremlin.scala.ScalaGraph
import gremlin.scala._
import shapeless.tag

class QualityService(qualityAdjuster: ConsecutiveQualityAdjuster, db: () => ScalaGraph) {

  def handleNewPomodoroQuality(quality: Int) = {
    db().addVertex(PomodoroQuality(Instant.now(), tag[Quality](quality)))
    val result = qualityAdjuster.apply()
    println(s"Adjustment result: $result")
  }

}
