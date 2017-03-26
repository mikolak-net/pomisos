package net.mikolak.pomisos.quality

import java.time.Instant

import smile.regression.ols
import gremlin.scala._
import net.mikolak.pomisos.data.ScalaGraphAccess

import scala.util.Try

class ConsecutiveQualityAdjuster(db: ScalaGraphAccess) extends QualityAdjuster {

  import ConsecutiveQualityAdjuster._

  protected[quality] def getData(): List[PomodoroQuality] =
    db(
      _.V
        .hasLabel[PomodoroQuality]
        .toCC[PomodoroQuality]
        .toList
        .sortBy(_.timestamp)(Ordering.fromLessThan(_ isBefore _))
        .takeRight(LastQualitiesCount))

  protected[quality] def predictWithData(lastQualities: List[PomodoroQuality]): Option[Double] =
    if (lastQualities.isEmpty) {
      None
    } else {
      if (lastQualities.map(_.quality).distinct.length == 1) {
        Some(lastQualities.head.quality)
      } else {
        Try {
          val OLS = ols(
            lastQualities.map(a => Array(a.timestamp.getEpochSecond.toDouble)).toArray,
            lastQualities.map(_.quality.toDouble).toArray
          )
          OLS.predict(
            Array(Instant.now().getEpochSecond.toDouble)
          )
        }.toOption
      }
    }

}

object ConsecutiveQualityAdjuster {

  val LastQualitiesCount = 5

}
