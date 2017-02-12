package net.mikolak.pomisos.quality

import java.time.{Instant, LocalDateTime, ZoneId}

import gremlin.scala.{ScalaGraph, _}
import net.mikolak.pomisos.data.DB
import smile.regression._

class TimeOfDayQuality(db: DB) {
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
