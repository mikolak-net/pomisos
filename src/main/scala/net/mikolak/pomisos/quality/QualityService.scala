package net.mikolak.pomisos.quality

import java.time.Instant

import gremlin.scala.ScalaGraph
import gremlin.scala._
import net.mikolak.pomisos.prefs.PreferenceDao
import shapeless.tag
import com.softwaremill.quicklens._
import scala.concurrent.duration._
import language.postfixOps

class QualityService(adjusters: List[QualityAdjuster], db: () => ScalaGraph, preferenceDao: PreferenceDao) {

  def handleNewPomodoroQuality(quality: Int) = {
    db().addVertex(PomodoroQuality(Instant.now(), tag[Quality](quality)))

    val current = preferenceDao.get().length

    val result = (adjusters.map(_(current).getOrElse(current.pomodoro.toMinutes.toInt)).sum / adjusters.size.toDouble).toInt

    preferenceDao.saveWith(_.modify(_.length.pomodoro).setTo(result minutes))

    println(s"Adjustment result: $result")
  }

}
