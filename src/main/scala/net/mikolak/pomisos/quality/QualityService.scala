package net.mikolak.pomisos.quality

import java.time.Instant

import gremlin.scala._
import net.mikolak.pomisos.prefs.PreferenceDao
import shapeless.tag
import com.softwaremill.quicklens._
import net.mikolak.pomisos.data.DB

import scala.concurrent.duration._
import language.postfixOps
import com.typesafe.scalalogging.Logger

class QualityService(adjusters: List[QualityAdjuster], db: DB, preferenceDao: PreferenceDao) {

  val log = Logger[QualityService]

  def handleNewPomodoroQuality(quality: Int) =
    if (preferenceDao.get().adaptive.enabled) {
      log.info(s"Obtained new quality rating: $quality")

      db().addVertex(PomodoroQuality(Instant.now(), tag[Quality](quality)))

      val current = preferenceDao.get().length

      val result = (adjusters.map(_(current).getOrElse(current.pomodoro.toMinutes.toInt)).sum / adjusters.size.toDouble).toInt

      preferenceDao.saveWith(_.modify(_.length.pomodoro).setTo(result minutes))
      log.info(s"Adjusted pomodoro length to: $result")
    } else {
      log.info("Adaptive adjustment disabled, length unchanged")
    }

}
