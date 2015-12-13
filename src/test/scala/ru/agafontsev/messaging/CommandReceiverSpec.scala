package ru.agafontsev.messaging

import java.net.InetSocketAddress

import akka.actor._
import akka.io.Udp
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class CommandReceiverSpec(_system: ActorSystem) extends TestKit(_system)
with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("CommandReceiverSpec", ConfigFactory.load().getConfig("single-system")))

  val inetAddress = InetSocketAddress.createUnresolved("127.0.0.1", 0)

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  "Command Receiver" should {
    "connect to m-cast group on startup" in {
      val ioProbe = TestProbe("ioUdp")
      val commandReceiver = system.actorOf(Props(classOf[CommandReceiver], ioProbe.ref, Props.empty, Props.empty, 1))
      ioProbe.expectMsgPF() {
        case Udp.Bind(`commandReceiver`, _, _) => // test pass
        case _ => fail()
      }
    }

    "start node on command 'start'" in {
      val nodeId: Int = 1
      val ioProbe = TestProbe("ioUdp")
      val nodeProbe = TestProbe("node")
      val responderProbe = TestProbe("responder")
      val commandReceiver = system.actorOf(Props(classOf[CommandReceiver], ioProbe.ref, Proxy.props(nodeProbe.ref), Proxy.props(responderProbe.ref),  nodeId))
      ioProbe.receiveN(1)
      ioProbe.reply(Udp.Bound(inetAddress))

      commandReceiver ! Udp.Received(ByteString("start"), inetAddress)
      system.actorSelection(s"/user/node-${nodeId}") ! Identify("getNode")
      expectMsgPF() {
        case ActorIdentity("getNode", Some(_)) => // passed
        case _ => fail()
      }
    }

    "stop node on command 'stop'" in {
      val nodeId: Int = 2
      val ioProbe = TestProbe("ioUdp")
      val nodeProbe = TestProbe("node")
      val responderProbe = TestProbe("responder")
      val commandReceiver = system.actorOf(Props(classOf[CommandReceiver], ioProbe.ref, Proxy.props(nodeProbe.ref), Proxy.props(responderProbe.ref),  nodeId))
      ioProbe.receiveN(1)
      ioProbe.reply(Udp.Bound(inetAddress))

      commandReceiver ! Udp.Received(ByteString("start"), inetAddress)
      commandReceiver ! Udp.Received(ByteString("stop"), inetAddress)

      system.actorSelection(s"/user/node-${nodeId}") ! Identify("getNode")
      expectMsgPF() {
        case ActorIdentity("getNode", None) => // passed
        case _ => fail()
      }
    }
  }
}

object Proxy {
  def props(probe: ActorRef): Props = Props(classOf[Proxy], probe)
}

class Proxy(probe: ActorRef) extends Actor {
  def receive = {
    case m => probe forward m
  }
}
