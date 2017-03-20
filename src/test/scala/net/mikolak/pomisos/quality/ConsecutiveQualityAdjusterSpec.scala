package net.mikolak.pomisos.quality

import java.time.Instant

import net.mikolak.pomisos.data.ScalaGraphAccess
import org.scalacheck.Gen

class ConsecutiveQualityAdjusterSpec extends QualitySpec {

  protected lazy val tested = new ConsecutiveQualityAdjuster(mock[ScalaGraphAccess])

  import Gen._
  import net.mikolak.pomisos.testutils.GensMore._

  protected def genInstant: Gen[List[Instant]] =
    structuredList(0l)(p => chooseNum[Long](p, Long.MaxValue))(_ + 1).map(_.map(Instant.ofEpochMilli))

  it must behave like monotonicAdjuster

}
