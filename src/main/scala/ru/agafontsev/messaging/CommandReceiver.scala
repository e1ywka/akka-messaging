package ru.agafontsev.messaging

import java.net._
import java.nio.channels.DatagramChannel
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.io.Inet.{DatagramChannelCreator, SocketOptionV2}
import akka.io.Udp
import akka.io.Udp.Bind
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.Try

/**
 * Получение и обработка команд управления.
 */
object CommandReceiver {
  trait Command

  /**
   * Изменение интервала генерации сообщений.
   * Не имеет эффекта, если нода не запущена.
   * @param ts новый интервал генерации сообщений.
   */
  case class ChangeTs(ts: FiniteDuration) extends Command

  /**
   * Остановить генерацию и получение сообщений.
   */
  case object StopNode extends Command

  case class StopNode(id: Int) extends Command

  /**
   * Запустить генерацию и получение сообщений.
   */
  case object StartNode extends Command

  case class StartNode(id: Int) extends Command

  /**
   * Запрос количества обрабатываемых сообщений в секунду.
   */
  case object GetAverage extends Command

  /**
   * Разбор команд управления.
   * @param command необработанный текст команды.
   * @return команда Command, или None, если синтаксис не верен.
   */
  def parseCommand(command: String): Option[Command] = command match {
    case period if period.startsWith("ts ") =>
      Try {
        Duration(period.substring(3, period.length))
      }.toOption.map { d =>
        ChangeTs(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS))
      }
    case "avg" => Some(GetAverage)
    case "start" => Some(StartNode)
    case start if start startsWith "start " =>
      Try(start.substring("start ".length, start.length).toInt).toOption map { i =>
        StartNode(i)
      }
    case "stop" => Some(StopNode)
    case stop if stop startsWith "stop " =>
      Try(stop.substring("stop ".length, stop.length).toInt).toOption map { i =>
        StopNode(i)
      }
    case _ => None
  }

  def multicastOptions(settings: SettingsImpl) = {
    List(Inet4ProtocolFamily(), MulticastGroup(settings.PingMulticastGroup, settings.PingMulticastInterface))
  }
}

/**
 * Получение и обработка команд управления.
 */
class CommandReceiver (ioUdp: ActorRef, nodeProps: Props, responderProps: Props, nodeId: Int) extends Actor with ActorLogging {
  import CommandReceiver._

  implicit val timeout = Timeout(3 seconds)
  implicit val ec = context.system.dispatcher
  implicit val system = context.system
  val settings = Settings(system)
  var node: Option[ActorRef] = None

  def receive = {
    case Udp.Bound(addr) =>
      log.debug("udp multicast bound")
      context become connected(sender)
  }

  def connected(socket: ActorRef): Receive = {
    case Udp.Received(data, _) =>
      val commandText = data.utf8String.trim
      log.debug(s"Received command ${commandText}")
      parseCommand(commandText) match {
        case Some(StartNode) =>
          if (node.isEmpty) {
            node = Some(context.watch(system.actorOf(nodeProps, s"node-${nodeId}")))
          }
        case Some(StartNode(id)) =>
          if (node.isEmpty && nodeId <= id) {
            node = Some(context.watch(system.actorOf(nodeProps, s"node-${nodeId}")))
          }
        case Some(StopNode) =>
          if (node.isDefined) {
            node.get ! PoisonPill
          }
        case Some(StopNode(id)) =>
          if (node.isDefined && nodeId > id) {
            node.get ! PoisonPill
          }
        case Some(GetAverage) =>
          if (node.isDefined) {
            val resp = context.actorOf(responderProps)
            node.get ? GetAverage pipeTo resp
          }
        case Some(c: Command) =>
          if (node.isDefined) {
            node.get ! c
          }
        case None => log.warning(s"Unrecognized command ${commandText}")
      }
    case Udp.CommandFailed(_) => context stop(self)
    case Udp.Unbind =>
      socket ! Udp.Unbind
      context become stopping(sender)
    case Terminated(_) => node = None
  }

  def stopping(respondTo: ActorRef): Receive = {
    case Udp.Unbound =>
      respondTo ! Udp.Unbound
      context.stop(self)
    case _ => sender ! Status.Failure(new IllegalStateException("Stopping"))
  }

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    ioUdp ! Bind(self, new InetSocketAddress(settings.PingMulticastPort), multicastOptions(settings))
  }
}

final case class Inet4ProtocolFamily() extends DatagramChannelCreator {
  override def create() = {
    DatagramChannel.open(StandardProtocolFamily.INET)
      .setOption[java.lang.Boolean](StandardSocketOptions.SO_REUSEADDR, true)
  }
}

final case class MulticastGroup(address: String, interface: String) extends SocketOptionV2 {
  override def afterBind(s: DatagramSocket) {
    val group = InetAddress.getByName(address)
    val networkInterface = NetworkInterface.getByName(interface)
    s.getChannel.join(group, networkInterface)
  }
}