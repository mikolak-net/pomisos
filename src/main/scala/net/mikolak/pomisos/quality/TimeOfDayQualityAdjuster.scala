package net.mikolak.pomisos.quality

import java.time.{Clock, Instant, LocalDateTime}

import gremlin.scala._
import net.mikolak.pomisos.data.ScalaGraphAccess
import smile.regression.ols

import scala.util.Try

class TimeOfDayQualityAdjuster(db: ScalaGraphAccess, clock: Clock) extends QualityAdjuster {
  protected[quality] def getData(): List[PomodoroQuality] =
    db(
      _.V
        .hasLabel[PomodoroQuality]
        .toCC[PomodoroQuality]
        .toList)

  protected[quality] def predictWithData(lastQualities: List[PomodoroQuality]): Option[Double] = {
    val currentHour = toHour(Instant.now(clock))
    if (lastQualities.isEmpty) {
      None
    } else {
      Try {
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
