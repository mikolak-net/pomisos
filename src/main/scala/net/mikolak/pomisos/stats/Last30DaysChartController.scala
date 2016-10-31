package net.mikolak.pomisos.stats

import java.time.{LocalDate, LocalDateTime, ZoneId}

import gremlin.scala.ScalaGraph
import net.mikolak.pomisos.data.PomodoroRun

import scalafxml.core.macros.sfxml
import scalafx.Includes._
import scalafx.scene.chart.{BarChart, XYChart}
import gremlin.scala._

import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.scene.layout.VBox

@sfxml
class Last30DaysChartController(val main: VBox,
                                chart: BarChart[String, Int],
                                db: () => ScalaGraph) {


  lazy val byDayCount = db().V.hasLabel[PomodoroRun].toCC[PomodoroRun]
    .toList
    .groupBy(v => LocalDate.from(LocalDateTime.ofInstant(v.endTime, ZoneId.systemDefault()))).map { case (day, verts) => (day, verts.size) }
    .withDefaultValue(0)

  val now = LocalDate.now()

  lazy val last30days = (0l to 29l).map(now.minusDays).reverse

  lazy val allDayCount = last30days.map(day => (day, byDayCount(day)))

  lazy val buffer = ObservableBuffer(allDayCount.map { case (k, v) => XYChart.Data(k.toString, v) })

  lazy val series = XYChart.Series[String, Int](buffer)

  chart.data = series

  def closeStats(event: ActionEvent): Unit = {
    main.visible.value = false
  }

}
