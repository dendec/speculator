package model

import java.util.Date
import javax.inject.Inject

import _root_.redis.ByteStringFormatter
import akka.actor.ActorSystem
import akka.util.ByteString
import controllers.Converter
import model.redis.RedisSortedSet
import org.msgpack.ScalaMessagePack
import org.msgpack.annotation.Message
import play.api.libs.json.{Json, Format}
import services.clients.ExchangeClient

import scala.concurrent.duration.Duration

/**
  * Created by denis on 3/16/16.
  */
@Message
class Candlestick(var open: Double, var close: Double, var high: Double, var low: Double, var volume: Double, var date: Date) extends Identifiable[Double] {

  def this() = this(0.0, 0.0, 0.0, 0.0, 0.0, null)

  override def getId: Double = date.getTime.toDouble

  override def setId(id: Double) = date = new Date(id.toLong)

  override def toString = s"Candlestick($open, $close, $high, $low, $volume, $date)"
}

object Candlestick {
  def getCollectionName(exchangeClient: ExchangeClient, duration: Duration) =
    s"${exchangeClient.exchange.getCollectionName}_${duration.toMinutes}m"
}

class CandlestickDAO @Inject() (val akkaSystem: ActorSystem) extends RedisSortedSet[Candlestick]{
  override implicit val byteStringFormatter: ByteStringFormatter[Candlestick] = new ByteStringFormatter[Candlestick] {
    def serialize(data: Candlestick): ByteString = {
      ByteString(ScalaMessagePack.write(data))
    }

    def deserialize(bs: ByteString): Candlestick = {
      ScalaMessagePack.read[Candlestick](bs.toArray)
    }
  }
}

case class JsonCandlestick(open: Double, close: Double, high: Double, low: Double, volume: Double, date: Date)

object JsonCandlestick {
  implicit val format: Format[JsonCandlestick] = Json.format[JsonCandlestick]

  def convert(candlestick: Candlestick): JsonCandlestick =
    JsonCandlestick(candlestick.open, candlestick.close, candlestick.high, candlestick.low, candlestick.volume, candlestick.date)
}