package ru.agafontsev.messaging

import java.net.InetSocketAddress

import akka.actor._
import akka.io.Udp
import akka.util.ByteString

/**
 * Класс выполняет пересылку сервисных сообщений.
 * @param ioUdp интерфейс Udp.
 * @param nodeId идентификатор текущей ноды.
 */
class Respond(ioUdp: ActorRef, nodeId: Int) extends Actor with ActorLogging with Stash {

  val settings = Settings(context.system)

  def receive: Receive = {
    case Node.Average(_) => stash()
    case Udp.SimpleSenderReady =>
      log.debug("Respond connected")
      unstashAll()
      context become ready(sender())
  }

  def ready(socket: ActorRef): Receive = {
    case Node.Average(average) =>
      log.debug(s"Sends average ${average}")
      socket ! Udp.Send(ByteString(s"node-${nodeId} ${average}\n", "UTF-8"), new InetSocketAddress(settings.ServiceHost, settings.ServicePort))
      socket ! PoisonPill
      context stop(self)
  }

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    ioUdp ! Udp.SimpleSender
  }
}
