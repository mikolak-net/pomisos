package net.mikolak.pomisos.quality

import java.time.Instant

import net.mikolak.pomisos.QualityAdjuster
import net.mikolak.pomisos.data.DB
import smile.regression.ols
import gremlin.scala._

class ConsecutiveQualityAdjuster(db: DB) extends QualityAdjuster {

  import ConsecutiveQualityAdjuster._

  def predict(): Option[Double] = {
    val lastQualities = db().V
      .hasLabel[PomodoroQuality]
      .toCC[PomodoroQuality]
      .toList
      .sortBy(_.timestamp)(Ordering.fromLessThan(_ isBefore _))
      .takeRight(LastQualitiesCount)
    predictWithData(lastQualities)
  }

  private[quality] def predictWithData(lastQualities: List[PomodoroQuality]): Option[Double] =
    if (lastQualities.isEmpty) {
      None
    } else {
      //TODO clean this up - breaks for LastQualitiesCount identical consecutive values (esp. 10)
      if (lastQualities.map(_.quality).distinct.length == 1) {
        Some(lastQualities.head.quality)
      } else {
        val OLS = ols(
          lastQualities.map(a => Array(a.timestamp.getEpochSecond.toDouble)).toArray,
          lastQualities.map(_.quality.toDouble).toArray
        )
        Some(
          OLS.predict(
            Array(Instant.now().getEpochSecond.toDouble)
          )
        )
      }
    }

}

object ConsecutiveQualityAdjuster {

  val LastQualitiesCount = 5

}