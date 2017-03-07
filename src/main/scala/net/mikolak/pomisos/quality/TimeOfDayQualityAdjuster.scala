package net.mikolak.pomisos.quality

import java.time.{Clock, Instant, LocalDateTime}

import gremlin.scala._
import net.mikolak.pomisos.data.DB
import smile.regression.ols

import scala.util.Try

class TimeOfDayQualityAdjuster(db: DB, clock: Clock) extends QualityAdjuster {
  protected[quality] def getData(): List[PomodoroQuality] =
    db().V
      .hasLabel[PomodoroQuality]
      .toCC[PomodoroQuality]
      .toList

  protected[quality] def predictWithData(lastQualities: List[PomodoroQuality]): Option[Double] = {
    val currentHour = toHour(Instant.now(clock))
    if (lastQualities.isEmpty) {
      None
    } else {
      Try { //TODO: fragile for some values
        val OLS = ols(
          lastQualities.map(a => Array(toHour(a.timestamp))).toArray,
          lastQualities.map(_.quality.toDouble).toArray
        )
        OLS.predict(
          Array(currentHour)
        )
      }.toOption
    }
  }

  private[quality] def toHour(ins: Instant) = LocalDateTime.ofInstant(ins, clock.getZone).getHour.toDouble

}
