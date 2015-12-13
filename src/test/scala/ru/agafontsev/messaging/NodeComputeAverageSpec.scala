package ru.agafontsev.messaging

import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable.{Buffer, ListBuffer}

class NodeComputeAverageSpec extends FlatSpec with Matchers {

  "Sending 1 message a second" should "give average 1 m/sec" in {
    var average = 0L
    var total: Seq[Long] = Seq.fill(100)(100L) ++ Seq.fill(100)(200L)
    var pings: Buffer[Long] = ListBuffer.empty
    total.indices foreach { messageOrderNum =>
      pings += total(messageOrderNum)
      Node.computeAveragePerSecond(pings) match {
        case (av, ps) => average = av; pings = ps
      }
    }
    println(average)
  }

}
