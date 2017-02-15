package net.mikolak.pomisos.quality

import java.time.Instant

import gremlin.scala._
import net.mikolak.pomisos.prefs.PreferenceDao
import shapeless.tag
import com.softwaremill.quicklens._
import net.mikolak.pomisos.QualityAdjuster
import net.mikolak.pomisos.data.DB

import scala.concurrent.duration._
import language.postfixOps

class QualityService(adjusters: List[QualityAdjuster], db: DB, preferenceDao: PreferenceDao) {

  def handleNewPomodoroQuality(quality: Int) = {
    db().addVertex(PomodoroQuality(Instant.now(), tag[Quality](quality)))

    val current = preferenceDao.get().length

    val result = (adjusters.map(_(current).getOrElse(current.pomodoro.toMinutes.toInt)).sum / adjusters.size.toDouble).toInt

    if (preferenceDao.get().adaptive.enabled) {
      preferenceDao.saveWith(_.modify(_.length.pomodoro).setTo(result minutes))
      println(s"Adjustment result: $result")
    } else {
      println(s"Adaptive adjustment disabled, length unchanged")
    }
  }

}
