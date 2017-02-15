package net.mikolak.pomisos.quality

import java.time.Instant

import com.fortysevendeg.scalacheck.datetime.jdk8.GenJdk8
import net.mikolak.pomisos.data.DB
import org.scalacheck.Gen
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FlatSpec, MustMatchers, OptionValues}
import shapeless.tag

import scala.util.Random

class ConsecutiveQualitySpec
    extends FlatSpec
    with MustMatchers
    with GeneratorDrivenPropertyChecks
    with MockitoSugar
    with OptionValues {

  val tested = new ConsecutiveQualityAdjuster(mock[DB])

  import com.fortysevendeg.scalacheck.datetime.jdk8._
  //TODO: this doesn't work as it should (is nonmonotonic for some reason)
  def genInstant = Gen.posNum[Long].flatMap(previous => Gen.chooseNum(previous, Long.MaxValue)).map(Instant.ofEpochMilli)
  def genQualityList(qualityGen: Gen[Int]) =
    Gen
      .nonEmptyListOf(Gen.zip(genInstant, qualityGen.map(tag[Quality].apply)).map((PomodoroQuality.apply _).tupled))

  it must "return None for empty input" in {
    tested.predictWithData(List.empty[PomodoroQuality]) must be(None)
  }

  it must "decrease monotonically with lower ratings" in {

    val Min = 1
    val Max = 10
    //TODO: merge XQuality with XQualityAdjuster, adjust test

    def monotonicDecrease = Gen.chooseNum[Int](Min, Max).flatMap(previous => Gen.chooseNum[Int](Min, previous))
    forAll(genQualityList(monotonicDecrease), minSuccessful(40)) { qualityList =>
      whenever(qualityList.size > 10) {
        val prediction = tested.predictWithData(qualityList)
        println(prediction)
        prediction.value must be < (Max - Min) / 2.0
      }
    }

  }

  it must "increase monotonically with higher ratings" in {

    val Min = 1
    val Max = 10

    def monotonicIncrease = Gen.chooseNum[Int](Min, Max).flatMap(previous => Gen.chooseNum[Int](previous, Max))
    forAll(genQualityList(monotonicIncrease), minSuccessful(40)) { qualityList =>
      whenever(qualityList.size > 10) {
        val prediction = tested.predictWithData(qualityList)
        println(prediction)
        prediction.value must be > (Max - Min) / 2.0
      }
    }

  }

}

object Test extends App {

  import org.scalacheck.Gen._

  val consequentNumber = {

    val random      = Random
    var previousNum = random.nextInt()

    () =>
      {
        val current = random.nextInt(Int.MaxValue - previousNum) + previousNum
        previousNum = current
        current
      }
  }

  val testGen = delay(consequentNumber())

  for (_ <- 1 to 10) {
    println(testGen.sample)
  }

}
