package net.mikolak.pomisos.dependencies

import com.softwaremill.macwire._
import net.mikolak.pomisos.quality._

trait QualityModule {
  this: DbModule =>

  lazy val consecutiveQuality = wire[ConsecutiveQuality]
  lazy val timeOfDayQuality = wire[TimeOfDayQuality]

  lazy val adjusters: List[QualityAdjuster] = List(wire[ConsecutiveQualityAdjuster], wire[TimeOfDayQualityAdjuster])

  lazy val qualityService = wire[QualityService]

}
