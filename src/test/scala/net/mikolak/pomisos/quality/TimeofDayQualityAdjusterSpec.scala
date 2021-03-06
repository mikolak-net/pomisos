package net.mikolak.pomisos.quality

import java.time.{Clock, Instant, ZoneId}

import org.scalacheck.Gen
import com.fortysevendeg.scalacheck.datetime.jdk8.GenJdk8
import com.fortysevendeg.scalacheck.datetime.jdk8.granularity.minutes
import net.mikolak.pomisos.data.ScalaGraphAccess

class TimeofDayQualityAdjusterSpec extends QualitySpec {

  private val testClock = Clock.fixed(Instant.now(), ZoneId.of("UTC"))

  protected lazy val tested = new TimeOfDayQualityAdjuster(mock[ScalaGraphAccess], testClock)

  import Gen._

  private val TestHour = tested.toHour(testClock.instant())

  protected def genInstant: Gen[List[Instant]] =
    listOf(
      GenJdk8
        .genZonedDateTime(minutes)
        .map(_.withZoneSameInstant(testClock.getZone).withHour(TestHour.toInt).toInstant))

  it must behave like monotonicAdjuster

}
