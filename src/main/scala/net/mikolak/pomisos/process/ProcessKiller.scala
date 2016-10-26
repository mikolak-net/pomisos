package net.mikolak.pomisos.process

class ProcessManager {

  def processFor(cmd: String) = ManagedProcess(cmd)

}

import scala.concurrent.Future
import scala.language.postfixOps
import scala.sys.process._

case class ManagedProcess(cmd: String) {

  import scala.concurrent.ExecutionContext.Implicits.global

  private def running_?() = (s"pgrep $cmd" !) == 0

  def kill() = Future(if(running_?()) {s"pkill $cmd" !})

  def create() = Future(if(!running_?()) {s"nohup $cmd" !})

}
