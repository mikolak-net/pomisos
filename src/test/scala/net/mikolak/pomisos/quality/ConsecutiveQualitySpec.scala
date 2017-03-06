package net.mikolak.pomisos.quality

import java.time.Instant

import net.mikolak.pomisos.data.DB
import org.scalacheck.Gen
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FlatSpec, MustMatchers, OptionValues}
import shapeless.tag

class ConsecutiveQualitySpec
    extends FlatSpec
    with MustMatchers
    with GeneratorDrivenPropertyChecks
    with MockitoSugar
    with OptionValues {

  val tested = new ConsecutiveQualityAdjuster(mock[DB])

  import Gen._
  import net.mikolak.pomisos.testutils.GensMore._

  def genInstant             = structuredList(0l)(p => chooseNum[Long](p, Long.MaxValue))(_ + 1).map(_.map(Instant.ofEpochMilli))
  def genQuality(start: Int) = structuredList[Int, Int](start) _

  def genQualityList(start: Int, gen: Int => Gen[Int], qualityStep: Int => Int) = sized { size =>
    zip(resize(size, genInstant), resize(size, genQuality(start)(gen)(qualityStep))).map {
      case (dates, qualities) => dates.zip(qualities.map(tag[Quality].apply)).map((PomodoroQuality.apply _).tupled)
    }
  }

  it must "return None for empty input" in {
    tested.predictWithData(List.empty[PomodoroQuality]) must be(None)
  }

  it must "decrease monotonically with lower ratings" in {

    forAll(genQualityList(Quality.Max, i => choose(Quality.Min, i), i => (i - 1).max(Quality.Min)), minSuccessful(40)) {
      qualityList =>
        whenever(qualityList.size > 10) {
          val prediction = tested.predictWithData(qualityList)
          prediction.value must be <= qualityList.head.quality.toDouble
        }
    }

  }

  it must "increase monotonically with higher ratings" in {

    forAll(genQualityList(Quality.Min, i => choose(i, Quality.Max), i => (i + 1).min(Quality.Max)), minSuccessful(40)) {
      qualityList =>
        whenever(qualityList.size > 10) {
          val prediction = tested.predictWithData(qualityList)
          prediction.value must be >= qualityList.head.quality.toDouble
        }
    }

  }

}
