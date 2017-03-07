package net.mikolak.pomisos.dependencies

import com.softwaremill.macwire._
import net.mikolak.pomisos.quality.{QualityAdjuster, _}

trait QualityModule { this: DbModule with TimeModule =>

  lazy val adjusters: List[QualityAdjuster] = List(wire[ConsecutiveQualityAdjuster], wire[TimeOfDayQualityAdjuster])

  lazy val qualityService = wire[QualityService]

}
