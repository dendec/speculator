package controllers

import javax.inject.Inject

import model._
import model.redis.RedisSortedSet
import play.api.libs.json.Format
import play.api.mvc.Controller

/**
  * Created by denis on 3/24/16.
  */
class CandlesticksController  @Inject()(candlestickDAO: CandlestickDAO) extends Controller with CrudController[Candlestick, JsonCandlestick, JsonCandlestick]{
  override val persistence: RedisSortedSet[Candlestick] = candlestickDAO

  override val converter: Converter[Candlestick, JsonCandlestick, JsonCandlestick] = new Converter[Candlestick, JsonCandlestick, JsonCandlestick] {
    override def convertIn(input: JsonCandlestick): Candlestick =
      new Candlestick(input.open, input.close, input.high, input.low, input.volume, input.date)

    override def convertOut(input: Candlestick): JsonCandlestick = JsonCandlestick.convert(input)
  }

  override implicit val inputFormat: Format[JsonCandlestick] = JsonCandlestick.format
  override implicit val outputFormat: Format[JsonCandlestick] = JsonCandlestick.format
}
