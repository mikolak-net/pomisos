package net.mikolak.pomisos.quality

import java.time.Instant

import org.scalacheck.Gen
import org.scalactic.anyvals.PosInt
import org.scalatest.{FlatSpec, MustMatchers, OptionValues}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import shapeless.tag

abstract class QualitySpec
    extends FlatSpec
    with MustMatchers
    with GeneratorDrivenPropertyChecks
    with MockitoSugar
    with OptionValues {

  private val Tries: PosInt = 40

  protected def tested: QualityAdjuster
  protected def genInstant: Gen[List[Instant]]

  import Gen._
  import net.mikolak.pomisos.testutils.GensMore._

  def genQuality(start: Int) = structuredList[Int, Int](start) _

  def genQualityList(start: Int, gen: Int => Gen[Int], qualityStep: Int => Int) = sized { size =>
    zip(resize(size, genInstant), resize(size, genQuality(start)(gen)(qualityStep))).map {
      case (dates, qualities) => dates.zip(qualities.map(tag[Quality].apply)).map((PomodoroQuality.apply _).tupled)
    }
  }

  def monotonicAdjuster = {

    it must "return None for empty input" in {
      tested.predictWithData(List.empty[PomodoroQuality]) must be(None)
    }

    it must "decrease monotonically with lower ratings, where applicable" in {

      //uniform gen not so great here, since it converges much faster to lower bound than it should, but good for now
      forAll(genQualityList(Quality.Max, i => choose(Quality.Min, i), _.max(Quality.Min)), minSuccessful(Tries)) { qualityList =>
        whenever(qualityList.size > 10) {
          val prediction = tested.predictWithData(qualityList)
          if (prediction.nonEmpty) { //TODO: defragile
            prediction.value.floor must be <= qualityList.head.quality.toDouble
          }
        }
      }

    }

    it must "increase monotonically with higher ratings, where applicable" in {

      //uniform gen not so great here, since it converges much faster to lower bound than it should, but good for now
      forAll(genQualityList(Quality.Min, i => choose(i, Quality.Max), _.min(Quality.Max)), minSuccessful(Tries)) { qualityList =>
        whenever(qualityList.size > 10) {
          val prediction = tested.predictWithData(qualityList)
          if (prediction.nonEmpty) { //TODO: defragile
            prediction.value.ceil must be >= qualityList.head.quality.toDouble
          }
        }
      }

    }

  }
}
