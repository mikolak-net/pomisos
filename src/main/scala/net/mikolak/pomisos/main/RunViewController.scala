package net.mikolak.pomisos.main

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter

import akka.actor.{Actor, ActorSystem, Cancellable, Props}
import gremlin.scala.ScalaGraph
import gremlin.scala._
import net.mikolak.pomisos.data.Pomodoro
import net.mikolak.pomisos.prefs.{Command, Preference}
import net.mikolak.pomisos.process.ProcessManager
import net.mikolak.pomisos.utils.{Implicits, Notifications}

import scalafxml.core.macros.sfxml
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.binding.{Bindings, BooleanBinding}
import scalafx.beans.property.{LongProperty, ObjectProperty}
import scalafx.scene.text.Text
import scala.concurrent.duration._
import language.postfixOps
import scalafx.event.ActionEvent

trait RunView {

  def runningPeriod: ObjectProperty[Option[TimerPeriod]]

  def remainingSeconds: LongProperty

  def runningPomodoro: ObjectProperty[Option[Pomodoro]]

  def isRunning: BooleanBinding

  def updateRunning(item: Option[TimerPeriod])

}

@sfxml
class RunViewController(val currentPomodoroDisplay: Text,
                        val timerText: Text,
                       val actorSystem: ActorSystem,
                        notifications: Notifications,
                        processMan: ProcessManager,
                        db: ScalaGraph) extends RunView {

  lazy val runningPeriod: ObjectProperty[Option[TimerPeriod]] = ObjectProperty(None)
  lazy val runningPomodoro = ObjectProperty[Option[Pomodoro]](None)

  lazy val remainingSeconds = LongProperty(0)
  lazy val isRunning = new BooleanBinding(remainingSeconds =!= 0)

  private var timerStack = List.empty[TimerPeriod]
  private var pomodoroCounter = 0
  val BreakText = "Break"

  override def updateRunning(item: Option[TimerPeriod]) = {
    runningPeriod.value = item
    item.foreach {
      pom =>
        remainingSeconds.value = pom.duration.toSeconds
        timerActor ! Start
    }
  }

  def stopPomodoro(event: ActionEvent) = {
    timerActor ! Stop
  }

  def pauseResume(event: ActionEvent) = {
    timerActor ! PauseResume
  }

  currentPomodoroDisplay.text <== Bindings.createStringBinding(() => runningPeriod.value.map(_.name).getOrElse(""), runningPeriod)

  lazy val formatter = DateTimeFormatter.ofPattern("mm:ss")
  timerText.text <== Bindings.createStringBinding(() => formatter.format(
    LocalDateTime.ofEpochSecond(remainingSeconds.toLong, 0, ZoneOffset.UTC)), remainingSeconds)

  private lazy val timerActor = actorSystem.actorOf(Props(classOf[TimerActor], remainingSeconds))

  //TODO: TEMP
  private def processes = db.V.hasLabel[Command].toCC[Command].map(cmd => processMan.processFor(cmd.cmd)).toList

  remainingSeconds.onChange((_, _, currentSeconds) => {
    if (currentSeconds.intValue() == 0) {
      popStack()
    }

    if (!isRunning.value && timerStack.isEmpty) {
      runningPomodoro.value = None
      notifications.show("Break done, pick a new Pomodoro!")
    }
  })

  isRunning.onChange((_, _, newVal) => {
    for (period <- runningPeriod.value if newVal) {
      val onBreak = period.name == BreakText
      val text = if (onBreak) "You're on a break" else period.name

      if(onBreak) { //TODO: TEMP
        processes.foreach(_.create())
      } else {
        pomodoroCounter+=1
        processes.foreach(_.kill())
      }

      notifications.show(text)
    }
  })


  runningPomodoro.onChange((obs, _, newVal) => for(newPomodoro <- newVal if newVal.isDefined) {
    println(s"Running Pomodoro $newPomodoro")
    val breakDuration = if (pomodoroCounter > 1 && (pomodoroCounter - 1) % Preference.current(db).pomodorosForLongBreak == 0) Preference.current(db).longBreakLength else Preference.current(db).shortBreakLength

    timerStack ::= TimerPeriod(None, BreakText, breakDuration)
    timerStack ::= TimerPeriod(Some(newPomodoro.id), newPomodoro.name, Preference.current(db).pomodoroLength)
    updateRunning(timerStack.headOption)
  })

  private def popStack() = {
    timerStack match {
      case ranPomodoro :: tail =>
        storeRun(ranPomodoro)
        timerStack = tail
        doRun()
      case _ => //do nothing
    }
  }

  private def doRun() = {
    updateRunning(timerStack.headOption)
  }

  private def storeRun(period: TimerPeriod) = {
    for {
      pomId <- period.id
      pomodoro <- db.V.hasLabel[Pomodoro].hasId(pomId).headOption() //TODO: this should be really one query
    } {
      val run = db.addVertex(PomodoroRun(Instant.now(), period.duration))
      pomodoro.addEdge("ranAt", run)
    }
  }

}


case class TimerActor(remainingSeconds: LongProperty) extends Actor {

  var currentSchedule: Option[Cancellable] = None

  override def receive = {
    case Start =>
      toggleScheduler()
    case Stop =>
      handleUpdate(0)
    case Tick =>
      handleUpdate(remainingSeconds.value - 1)
    case PauseResume =>
      toggleScheduler()
  }

  private def toggleScheduler() = {
    currentSchedule match {
      case Some(scheduler) =>
        scheduler.cancel()
        currentSchedule = None
      case None =>
        import context.dispatcher
        currentSchedule = Some(context.system.scheduler.schedule(1 seconds, 1 second, self, Tick))
    }
  }

  private def handleUpdate(secs: Long) = {
    if (secs == 0) {
      toggleScheduler()
    }
    Platform.runLater(remainingSeconds.value = secs)
  }
}

class AudioActor(db: ScalaGraph) extends Actor {



  override def receive = {

  }
}

case object Tick

case object Start

case object Stop

case object PauseResume