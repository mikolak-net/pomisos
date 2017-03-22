package net.mikolak.pomisos.process

import akka.actor.{Actor, ActorLogging, Cancellable, PoisonPill}

import scala.concurrent.Future

class DeferredExecutor extends Actor with ActorLogging with CommandExecution {
  import ExecutionLauncher._
  import DeferredExecutor._

  import language.postfixOps
  import scala.concurrent.duration._
  import context.dispatcher

  object Ping

  var runningProcess: Option[Future[CommandStatus]] = None
  var scheduleSubscription: Option[Cancellable]     = None

  var ignoredCodes = Set.empty[Int]

  override def receive: Receive = {
    case fullCommand: CommandDesc =>
      ignoredCodes ++= fullCommand._2
      runningProcess = Some(Future(execute(fullCommand._1)))
      scheduleSubscription = Some(context.system.scheduler.schedule(10 millis, 1 second, self, Ping))

    case Ping =>
      val currentResult = runningProcess.flatMap(_.value.flatMap(_.toOption))
      currentResult.collect {
        case Left(errorInfo) if !ignoredCodes.contains(errorInfo._1) => context.parent ! errorInfo
      }

      currentResult.flatMap(_ => scheduleSubscription).foreach { sub =>
        sub.cancel()
        self ! PoisonPill
      }

  }

}

object DeferredExecutor {
  type CommandDesc = (String, Set[Int])

}
