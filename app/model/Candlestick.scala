package model

import play.api.libs.json.{Json, Format}

/**
  * Created by denis on 3/16/16.
  */
case class Candlestick(open: Double, close: Double, high: Double, low: Double, volume: Double, date: Long)

object Candlestick {
  implicit val format: Format[Candlestick] = Json.format[Candlestick]
}