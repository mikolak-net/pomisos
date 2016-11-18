package net.mikolak.pomisos.quality

import java.time.Instant

import gremlin.scala.ScalaGraph
import gremlin.scala._
import smile.regression._

class ConsecutiveQuality (db: () => ScalaGraph) {
  def predict(): Option[Double] = {
    val last5Qualities = db().V
        .hasLabel[PomodoroQuality]
        .toCC[PomodoroQuality]
        .toList
        .sortBy(_.timestamp)(Ordering.fromLessThan(_ isBefore _))
        .takeRight(5)
    if (last5Qualities.isEmpty) {
      None
    } else {
      val OLS = ols(
        last5Qualities.map(a => Array(a.timestamp.getEpochSecond.toDouble)).toArray,
        last5Qualities.map(_.quality.toDouble).toArray
      )
      Some(
        OLS.predict(
          Array(Instant.now().getEpochSecond.toDouble)
        )
      )
    }

  }
}
