package net.mikolak.pomisos.process

import java.nio.file.Path

import akka.actor.Props
import akka.testkit.TestKit
import net.mikolak.pomisos.reporting.Error

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

class ExecutionLauncherSpec extends LauncherSpec {

  it must "execute the command on break" in {
    init(numKills = 1)

    tested ! OnBreak
    TestKit.awaitCond(target.hasStarted, DefaultTimeout, 10 millis)
  }

  it must "report a non-zero exit status on break" in {
    val statusCode = 40
    init(numKills = 0, statusCode = statusCode)

    expectReport {
      tested ! OnBreak
    } {
      case Error(s) => s.contains(s" $statusCode")
    }

  }

  it must "ignore the SIGTERM exit status" in {
    val statusCode = 143
    init(numKills = 1, statusCode = statusCode)

    tested ! OnBreak
    expectNoReport {
      tested ! OnPomodoro
    }
  }

  it must "stop the given command on pomodoro" in {
    val numKills = 1
    init(numKills)

    tested ! OnBreak
    tested ! OnPomodoro
    TestKit.awaitCond(target.numKills.contains(numKills), DefaultTimeout, 10 millis)
  }

  it must "attempt to kill the command several times" in {
    val numKills = ExecutionLauncher.KillsToTry - 1
    init(numKills)

    tested ! OnBreak
    tested ! OnPomodoro
    TestKit.awaitCond(target.numKills.contains(numKills), DefaultTimeout, 10 millis)
  }

  it must "give up and report failure if max kill attempts is exceeded" in {
    val numKills = ExecutionLauncher.KillsToTry + 1
    init(numKills)

    tested ! OnBreak
    expectReport {
      tested ! OnPomodoro
    } {
      case Error(s) => s.contains(ExecutionLauncher.KillsToTry.toString)
    }
  }

  private def init(numKills: Int = 1, statusCode: Int = 0) = {
    target = setupCommand(numKills, statusCode)

    tested = system.actorOf(Props(classOf[ExecutionLauncher], target.execution))
  }

  private def setupCommand(numKills: Int, statusCode: Int) = {
    val script = (dataFile: Path, startString: String) => {
      val dataFilePath = dataFile.toAbsolutePath.toString
      s"""#!/bin/bash
                   |
                   |KILLSLEFT=$numKills
                   |KILLSRECEIVED=0
                   |
                   |echo "$startString" > $dataFilePath
                   |
                   |_term() {
                   |  ((KILLSLEFT--))
                   |  ((KILLSRECEIVED++))
                   |
                   |  echo "$$KILLSRECEIVED" > $dataFilePath
                   |}
                   |
                   |trap _term SIGTERM
                   |
                   |while [ $$KILLSLEFT -gt 0 ]
                   |do
                   |true # used instead of sleep because otherwise kill status write nondeterministic
                   |done
                   |
                   |exit $statusCode""".stripMargin

    }

    LauncherTestProcess(script)
  }

  implicit class MultiKillTestProcess(process: LauncherTestProcess) {
    def numKills = Try(process.dataContent.toInt).toOption
  }
}
