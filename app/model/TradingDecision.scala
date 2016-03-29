package model

import java.util.Date
import javax.inject.Inject

import _root_.redis.ByteStringFormatter
import akka.actor.ActorSystem
import akka.util.ByteString
import model.redis.RedisSortedSet
import org.msgpack.ScalaMessagePack
import org.msgpack.annotation.Message
import play.api.libs.json.{Json, Format}

/**
  * Created by denis on 3/28/16.
  */
@Message
class TradingDecision(var date: Date, var action: Int, var fidelity: Double, var price: Double, var exchange: ExchangeDetails) extends Identifiable[Double] {

  def this() = this(null, 0, 0.0, 0.0, null)

  override def getId: Double = date.getTime.toDouble

  override def setId(id: Double) = date = new Date(id.toLong)

  override def toString = s"TradingDecision($date, $action, $fidelity, $price, $exchange)"
}

class TradingDecisionDAO @Inject() (val akkaSystem: ActorSystem) extends RedisSortedSet[TradingDecision]{

  override implicit val byteStringFormatter: ByteStringFormatter[TradingDecision] = new ByteStringFormatter[TradingDecision] {
    def serialize(data: TradingDecision): ByteString = {
      ByteString(ScalaMessagePack.write(data))
    }

    def deserialize(bs: ByteString): TradingDecision = {
      ScalaMessagePack.read[TradingDecision](bs.toArray)
    }
  }
}

case class JsonTradingDecision(date: Date, action: String, fidelity: Double, price: Double, exchange: JsonExchangeDetails)

object JsonTradingDecision {
  implicit val format: Format[JsonTradingDecision] = Json.format[JsonTradingDecision]
}