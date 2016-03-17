package controllers

import javax.inject.{Inject, Singleton}

import model._
import model.redis.RedisSortedSet
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{Json, Format}
import play.api.mvc.{Action, Controller}
import services.Calculator

import scala.concurrent.duration.Duration

/**
  * Created by denis on 3/11/16.
  */
@Singleton
class DealsController @Inject()(dealDao: DealDAO, calculator: Calculator) extends Controller with CrudController[Deal, JsonDeal, JsonDeal]{
  override val persistence: RedisSortedSet[Deal] = dealDao
  override val converter: Converter[Deal, JsonDeal, JsonDeal] = new Converter[Deal, JsonDeal, JsonDeal] {
    override def convertIn(input: JsonDeal): Deal = {
      val exchange = new ExchangeDetails(input.exchange.name, input.exchange.baseCurrency.id, input.exchange.tradeCurrency.id)
      new Deal(input.date, input.amount, input.price, exchange)
    }

    override def convertOut(input: Deal): JsonDeal = {
      val baseCurrency = ExchangeCurrency.apply(input.exchange.baseCurrency)
      val tradeCurrency = ExchangeCurrency.apply(input.exchange.tradeCurrency)
      val exchange = new JsonExchangeDetails(input.exchange.name, s"${baseCurrency.toString}->${tradeCurrency.toString}",
        baseCurrency, tradeCurrency, input.exchange.getCollectionName)
      JsonDeal(input.date, input.amount, input.price, exchange)
    }
  }

  override implicit val inputFormat: Format[JsonDeal] = JsonDeal.format
  override implicit val outputFormat: Format[JsonDeal] = JsonDeal.format

  def getCandlesticks(key: String, from: Option[Long], to: Option[Long], duration: Option[Int]) = Action.async {
    persistence.get(key, from.getOrElse(Long.MinValue), to.getOrElse(Long.MaxValue)).map{deals =>
      val candlesticks = calculator.getCandlesticks(Duration(duration.getOrElse(15), "minute"))(deals)
      Ok(Json.toJson(candlesticks))
    }
  }

  def getSMA(key: String, from: Option[Long], to: Option[Long], duration: Option[Int]) = Action.async {
    persistence.get(key, from.getOrElse(Long.MinValue), to.getOrElse(Long.MaxValue)).map{deals =>
      val candlesticks = calculator.getCandlesticks(Duration(duration.getOrElse(15), "minute"))(deals)
      val sma7 = calculator.simpleMovingAverage(candlesticks.map(_.close), 7)
      val sma15 = calculator.simpleMovingAverage(candlesticks.map(_.close), 15)
      val sma30 = calculator.simpleMovingAverage(candlesticks.map(_.close), 30)
      val timePoints = candlesticks.zipWithIndex.map {
        case (candlestick, index) =>
          val sma7value = if (sma7(index) == 0) sma7(6) else sma7(index)
          val sma15value= if (sma15(index) == 0) sma15(14) else sma15(index)
          val sma30value= if (sma30(index) == 0) sma30(29) else sma30(index)
          MAPoint(candlestick.date, sma7value, sma15value, sma30value)
      }
      Ok(Json.toJson(timePoints))
    }
  }

}
