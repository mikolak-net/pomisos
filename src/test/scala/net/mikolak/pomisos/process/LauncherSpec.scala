package net.mikolak.pomisos.process

import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.{Files, Path}

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestKit, TestProbe}
import net.mikolak.pomisos.prefs.Execution
import net.mikolak.pomisos.reporting.ReportingNotification
import org.scalatest._

import scala.concurrent.duration._
import scala.language.postfixOps

abstract class LauncherSpec
    extends TestKit(ActorSystem("TestSystem"))
    with FlatSpecLike
    with MustMatchers
    with BeforeAndAfter
    with BeforeAndAfterAll {

  protected val DefaultTimeout = 3 seconds

  protected var target: LauncherTestProcess = _

  protected var tested: ActorRef = _

  protected def expectReport(blockToExecute: => Unit)(cond: PartialFunction[Any, Boolean]) = {
    val probe = TestProbe()
    system.eventStream.subscribe(probe.ref, classOf[ReportingNotification])

    blockToExecute

    probe.expectMsgPF(max = DefaultTimeout)(cond) must be(true)
  }

  after {
    target.forceStop()
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

}

case class LauncherTestProcess(script: (Path, String) => String) {

  import LauncherTestProcess._

  import language.postfixOps
  import scala.collection.JavaConverters._
  import sys.process._

  val scriptFile =
    Files.createTempFile(TmpPrefix, "", PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr--r--")))

  val dataFile = Files.createTempFile("", "")

  Files.write(scriptFile, script(dataFile, StartString).split("\n").toIterable.asJava)

  def execution = Execution(None, Some(scriptFile.toAbsolutePath.toString))

  def dataContent = new String(Files.readAllBytes(dataFile)).trim()

  def hasStarted = dataContent == StartString

  def forceStop() = s"pkill -9 -f  ${scriptFile.toAbsolutePath.toString}" !

}

object LauncherTestProcess {

  private val StartString = "started"

  private val TmpPrefix = "pomisosTest"

}
