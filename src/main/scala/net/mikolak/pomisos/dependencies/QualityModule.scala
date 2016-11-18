package net.mikolak.pomisos.dependencies

import com.softwaremill.macwire._
import net.mikolak.pomisos.quality.{ConsecutiveQuality, ConsecutiveQualityAdjuster, QualityService}

trait QualityModule {
  this: DbModule =>

  lazy val consecutiveQuality = wire[ConsecutiveQuality]
  lazy val qualityAdjuster = wire[ConsecutiveQualityAdjuster]
  lazy val qualityService = wire[QualityService]

}
