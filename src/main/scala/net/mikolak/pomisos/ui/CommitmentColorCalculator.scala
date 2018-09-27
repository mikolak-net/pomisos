package net.mikolak.pomisos.ui
import scalafx.scene.paint.Color

//TODO: test
class CommitmentColorCalculator {

  import CommitmentColorCalculator._

  def apply(committed: Int, actual: Long): Color = (committed, actual.toInt) match {
    case (_, 0)           => Color.Black
    case (c, a) if c >= a => colorForHue(GoodHue)
    case (c, a) if c < a  => colorForHue((GoodHue - ((a - c) / CriticalFactor) * HueInterval).max(BadHue))
  }

  private def colorForHue(hue: Double) = Color.hsb(hue, 0.8, 0.7)

}

object CommitmentColorCalculator {
  val BadHue      = 0.0
  val GoodHue     = 121.0
  val HueInterval = GoodHue - BadHue

  val CriticalFactor = 5.0

}
