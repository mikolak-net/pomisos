package net.mikolak.pomisos.quality

import net.mikolak.pomisos.prefs.PreferenceDao
import com.softwaremill.quicklens._
import scala.concurrent.duration._
import language.postfixOps

class QualityAdjuster(consecutiveQuality: ConsecutiveQuality, preferenceDao: PreferenceDao) {

  import QualityAdjuster._

  def apply(): Option[Int] = {
      consecutiveQuality.predict().map ( quality => {
        val current = preferenceDao.get().length.pomodoro.toMinutes
        val newValue  = (current+(quality-Neutral)*FudgeFactor).toInt

        preferenceDao.saveWith(_.modify(_.length.pomodoro).setTo(newValue minutes))

        newValue
      })
  }

}

object QualityAdjuster {

  val Neutral = 7

  val FudgeFactor = 1.0

}
