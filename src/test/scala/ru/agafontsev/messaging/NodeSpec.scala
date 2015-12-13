package ru.agafontsev.messaging

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import ru.agafontsev.messaging.Node.Ping

import scala.concurrent.duration._

class NodeSpec(_system: ActorSystem) extends TestKit(_system)
with ImplicitSender with FlatSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("NodeSpec", ConfigFactory.load().getConfig("single-system")))

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  "Node" should "send Ping" in {
    val remoteNode = TestProbe()
    val nodesSelection = List(system.actorSelection(remoteNode.ref.path))
    val node = system.actorOf(Props(classOf[Node], 100 millis, nodesSelection))
    remoteNode.expectMsg(100 millis, Ping(node))
  }

  "Node" should "return average" in {
    val probe = TestProbe()
    val node = system.actorOf(Props(classOf[Node], 100 millis, Nil))
    probe.send(node, Ping(probe.ref))
    node ! CommandReceiver.GetAverage
    expectMsgPF() {
      case Node.Average(average) => average should be > 0L; println(average)
    }

  }


}
