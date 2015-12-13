package ru.agafontsev.messaging

import akka.actor._

import scala.collection.mutable
import scala.compat.Platform
import scala.concurrent.duration.{Duration, FiniteDuration}

object Node {
  case class Ping(sender: ActorRef)
  case class Average(a: Long)

  def computeAveragePerSecond(pings: mutable.Buffer[Long]): (Long, mutable.Buffer[Long]) = {
    if (pings.isEmpty) {
      return (0, pings)
    }
    if (pings.length == 1) {
      (1L, pings)
    } else {
      var reducedPings = pings
      if (pings.length >= 100) {
        reducedPings = pings.grouped(10).map(g => g.sum / g.length).toBuffer
      }
      ((reducedPings.length * 1000) / reducedPings.sum , reducedPings)
    }
  }
}


class Node(defaultTs: FiniteDuration, nodes: Seq[ActorSelection]) extends Actor with ActorLogging {
  import Node._

  implicit val ec = context.system.dispatcher

  var pingScheduler: Option[Cancellable] = None

  var lastMessageReceivedAtMillis: Long = Platform.currentTime
  var average: Long = 0
  var pingsReceivedAt: mutable.Buffer[Long] = mutable.Buffer.empty

  def receive = {
    case Ping(s) if !self.equals(s) =>
      val receivedAt = Platform.currentTime
      pingsReceivedAt += receivedAt - lastMessageReceivedAtMillis
      computeAveragePerSecond(pingsReceivedAt) match {
        case (newAverage, pings) =>
          average = newAverage
          pingsReceivedAt = pings
      }
      lastMessageReceivedAtMillis = receivedAt
      log.info(s"Current average: ${average}")
    case CommandReceiver.ChangeTs(newTs) =>
      if (pingScheduler.isDefined) {
        pingScheduler.get.cancel()
      }
      pingScheduler = Some(context.system.scheduler.schedule(newTs, newTs){
        nodes foreach (_ ! Ping(self))
      })
    case CommandReceiver.GetAverage => sender() ! Average(average)
  }

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.debug("node started")
    pingScheduler = Some(context.system.scheduler.schedule(Duration.Zero, defaultTs) {
      nodes foreach (_ ! Ping(self))
    })
  }



  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    log.debug("node stoped")
    if (pingScheduler.isDefined) {
      pingScheduler.get.cancel()
    }
  }
}
