package ru.agafontsev.messaging

import org.scalatest.{FlatSpec, Matchers}
import ru.agafontsev.messaging.CommandReceiver._

import scala.concurrent.duration._

class CommandReceiverParseSpec extends FlatSpec with Matchers {

  "Commands" should "be parsed successfully" in {
    CommandReceiver.parseCommand("ts 300 millis") should be(Some(ChangeTs(300 millis)))
    CommandReceiver.parseCommand("ts 1 second") should be(Some(ChangeTs(1000 millis)))
    CommandReceiver.parseCommand("ts 500 nanos") should be(Some(ChangeTs(500 nanos)))

    CommandReceiver.parseCommand("start") should be(Some(StartNode))
    CommandReceiver.parseCommand("start 1") should be (Some(StartNode(1)))

    CommandReceiver.parseCommand("stop") should be(Some(StopNode))
    CommandReceiver.parseCommand("stop 1") should be (Some(StopNode(1)))

    CommandReceiver.parseCommand("avg") should be(Some(GetAverage))
  }

}
