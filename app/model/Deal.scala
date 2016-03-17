package model

import java.util.Date
import javax.inject.Inject

import _root_.redis.ByteStringFormatter
import akka.actor.{ActorRefFactory, ActorSystem}
import akka.util.ByteString
import model.redis.RedisSortedSet
import org.msgpack.ScalaMessagePack
import org.msgpack.annotation.Message
import play.api.Logger
import play.api.libs.json.{Json, Format}

/**
  * Created by denis on 3/11/16.
  */
@Message
class Deal(var date: Date, var amount: Double, var price: Double, var exchange: ExchangeDetails) extends Identifiable[Double] {

  def this() = this(null, 0.0, 0.0, null)

  override def getId: Double = date.getTime.toDouble

  override def setId(id: Double) = date = new Date(id.toLong)

  override def toString = s"Deal($date, $amount, $price, $exchange)"
}

case class JsonDeal(date: Date, amount: Double, price: Double, exchange: JsonExchangeDetails)

object JsonDeal {
  implicit val format: Format[JsonDeal] = Json.format[JsonDeal]
}

class DealDAO @Inject() (val akkaSystem: ActorSystem) extends RedisSortedSet[Deal]{

  val defaultKey: String = "deals"

  override implicit val byteStringFormatter: ByteStringFormatter[Deal] = new ByteStringFormatter[Deal] {
    def serialize(data: Deal): ByteString = {
      ByteString(ScalaMessagePack.write(data))
    }

    def deserialize(bs: ByteString): Deal = {
      ScalaMessagePack.read[Deal](bs.toArray)
    }
  }
}