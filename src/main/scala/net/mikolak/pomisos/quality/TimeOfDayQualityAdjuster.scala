package net.mikolak.pomisos.quality

import java.time.{Instant, LocalDateTime, ZoneId}

import net.mikolak.pomisos.QualityAdjuster
import net.mikolak.pomisos.data.DB
import net.mikolak.pomisos.prefs.LengthPreferences
import smile.regression.ols
import gremlin.scala._

class TimeOfDayQualityAdjuster(db: DB) extends QualityAdjuster {
  import TimeOfDayQualityAdjuster._

  def predict(): Option[Double] = {
    val currentHour = toHour(Instant.now())

    val lastQualities = db().V
      .hasLabel[PomodoroQuality]
      .toCC[PomodoroQuality]
      .toList
    if (lastQualities.isEmpty) {
      None
    } else {

      val OLS = ols(
        lastQualities.map(a => Array(toHour(a.timestamp))).toArray,
        lastQualities.map(_.quality.toDouble).toArray
      )
      Some(
        OLS.predict(
          Array(currentHour)
        )
      )

    }
  }

  private def toHour(ins: Instant) = LocalDateTime.ofInstant(ins, ZoneId.systemDefault()).getHour.toDouble

}

object TimeOfDayQualityAdjuster {

  val Neutral     = 7
  val FudgeFactor = 1.0

}
