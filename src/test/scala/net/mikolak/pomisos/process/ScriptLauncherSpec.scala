package net.mikolak.pomisos.process

import java.nio.file.Path

import akka.actor.Props
import akka.testkit.TestKit
import net.mikolak.pomisos.prefs.Script
import net.mikolak.pomisos.reporting.Error

import scala.concurrent.duration._
import scala.language.postfixOps

class ScriptLauncherSpec extends LauncherSpec {

  def init(statusCode: Int, which: WhichScript) {
    target = setupCommand(statusCode)
    val script =
      s"""#!/bin/bash
         |${target.scriptFile.toAbsolutePath.toString}""".stripMargin

    val scriptSpec = which match {
      case PomodoroScript => Script(None, Some(script), None)
      case BreakScript    => Script(None, None, Some(script))
    }

    tested = system.actorOf(Props(classOf[ScriptLauncher], scriptSpec))
  }

  it must "execute the given command on break" in {
    init(0, BreakScript)

    tested ! OnBreak
    TestKit.awaitCond(target.hasStarted, DefaultTimeout, 10 millis)
  }

  it must "execute the given command on pomodoro" in {
    init(0, PomodoroScript)

    tested ! OnPomodoro
    TestKit.awaitCond(target.hasStarted, DefaultTimeout, 10 millis)
  }

  it must "report back when the provided command fails for break" in {
    val statusCode = 12
    init(statusCode, BreakScript)

    expectReport {
      tested ! OnBreak
    } {
      case Error(s) => s.contains(s" $statusCode")
    }
  }

  it must "report back when the provided command fails for pomodoro" in {
    val statusCode = 13
    init(statusCode, PomodoroScript)

    expectReport {
      tested ! OnPomodoro
    } {
      case Error(s) => s.contains(s" $statusCode")
    }
  }

  private def setupCommand(statusCode: Int) = {
    val script = (dataFile: Path, startString: String) => {
      val dataFilePath = dataFile.toAbsolutePath.toString
      s"""#!/bin/bash
         |echo "$startString" > $dataFilePath
         |exit $statusCode""".stripMargin
    }

    LauncherTestProcess(script)
  }
}

private sealed trait WhichScript

private object PomodoroScript extends WhichScript
private object BreakScript    extends WhichScript
