package ru.agafontsev.messaging

import akka.actor._
import akka.io.{IO, Udp}
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration._

object NodeMain {
  def main(args: Array[String]): Unit = {
    val nodeId = args(0).toInt
    val nodeCount = args(1).toInt
    implicit val actorSystem = ActorSystem(s"node-${nodeId}")
    implicit val ec = actorSystem.dispatcher

    val nodes: ArrayBuffer[ActorSelection] = ArrayBuffer.empty
    1 to nodeCount foreach { i =>
      if (i != nodeId) {
        nodes += actorSystem.actorSelection(s"akka.tcp://node-${i}@127.0.0.1:${2552 + i}/user/node-*")
      }
    }
    val nodeProps = Props(classOf[Node], 100 millis, nodes)
    val responderProps = Props(classOf[Respond], IO(Udp), nodeId)
    val connect = actorSystem.actorOf(Props(classOf[CommandReceiver], IO(Udp), nodeProps, responderProps, nodeId))

    sys.addShutdownHook {
      val terminated = for {
        _ <- connect.ask(Udp.Unbind)(Timeout(10 seconds))
        _ <- actorSystem.terminate()
      } yield Terminated
      Await.ready(terminated, 10 seconds)
    }
  }
}