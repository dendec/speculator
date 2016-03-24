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
      Ok(Json.toJson(candlesticks.map(JsonCandlestick.convert)))
    }
  }

  def getSMA(key: String, from: Option[Long], to: Option[Long], duration: Option[Int]) = Action.async {
    persistence.get(key, from.getOrElse(Long.MinValue), to.getOrElse(Long.MaxValue)).map { deals =>
      val candlesticks = calculator.getCandlesticks(Duration(duration.getOrElse(15), "minute"))(deals)
      val price = candlesticks.map(candlestick => TimePoint(candlestick.date, candlestick.close))
      val sma7 = calculator.simpleMovingAverageByCandlesticks(candlesticks, 7)
      val sma15 = calculator.simpleMovingAverageByCandlesticks(candlesticks, 15)
      val sma30 = calculator.simpleMovingAverageByCandlesticks(candlesticks, 30)
      Ok(Json.toJson(Map("price" -> price, "sma7" -> sma7, "sma15" -> sma15, "sma30" -> sma30)))
    }
  }

  def getLMA(key: String, from: Option[Long], to: Option[Long], duration: Option[Int]) = Action.async {
    persistence.get(key, from.getOrElse(Long.MinValue), to.getOrElse(Long.MaxValue)).map { deals =>
      val candlesticks = calculator.getCandlesticks(Duration(duration.getOrElse(15), "minute"))(deals)
      val price = candlesticks.map(candlestick => TimePoint(candlestick.date, candlestick.close))
      val lma7 = calculator.linearMovingAverageByCandlesticks(candlesticks, 7)
      val lma15 = calculator.linearMovingAverageByCandlesticks(candlesticks, 15)
      val lma30 = calculator.linearMovingAverageByCandlesticks(candlesticks, 30)
      Ok(Json.toJson(Map("price" -> price, "lma7" -> lma7, "ema15" -> lma15, "ema30" -> lma30)))
    }
  }

  def getMACD(key: String, from: Option[Long], to: Option[Long], duration: Option[Int]) = Action.async {
    persistence.get(key, from.getOrElse(Long.MinValue), to.getOrElse(Long.MaxValue)).map { deals =>
      val candlesticks = calculator.getCandlesticks(Duration(duration.getOrElse(15), "minute"))(deals)
      Ok(Json.toJson(calculator.getMACDByCandlesticks(candlesticks)))
    }
  }

  /*def getEMA(key: String, from: Option[Long], to: Option[Long], duration: Option[Int]) = Action.async {
    persistence.get(key, from.getOrElse(Long.MinValue), to.getOrElse(Long.MaxValue)).map{deals =>
      val candlesticks = calculator.getCandlesticks(Duration(duration.getOrElse(15), "minute"))(deals)
      val ema7 = calculator.exponentialMovingAverage(candlesticks.map(_.close), 7)
      val ema15 = calculator.exponentialMovingAverage(candlesticks.map(_.close), 15)
      val ema30 = calculator.exponentialMovingAverage(candlesticks.map(_.close), 30)
      val timePoints = candlesticks.zipWithIndex.map {
        case (candlestick, index) =>
          val sma7value = if (ema7(index) == 0) ema7(6) else ema7(index)
          val sma15value= if (ema15(index) == 0) ema15(14) else ema15(index)
          val sma30value= if (ema30(index) == 0) ema30(29) else ema30(index)
          MAPoint(candlestick.date, candlestick.open, sma7value, sma15value, sma30value)
      }
      Ok(Json.toJson(timePoints))
    }
  }

  def getMACD(key: String, from: Option[Long], to: Option[Long], duration: Option[Int]) = Action.async {
    persistence.get(key, from.getOrElse(Long.MinValue), to.getOrElse(Long.MaxValue)).map{deals =>
      val candlesticks = calculator.getCandlesticks(Duration(duration.getOrElse(15), "minute"))(deals)
      val ema12 = calculator.exponentialMovingAverage(candlesticks.map(_.close), 12)
      val ema26 = calculator.exponentialMovingAverage(candlesticks.map(_.close), 26)
      val timePoints = candlesticks.zipWithIndex.map {
        case (candlestick, index) =>
          val sma12value = if (ema12(index) == 0) ema12(11) else ema12(index)
          val sma26value= if (ema26(index) == 0) ema26(25) else ema26(index)
          val signal = sma12value - sma26value
          null
          //MAPoint(candlestick.date, candlestick.open, sma7value, sma15value, sma30value)
      }
      Ok(Json.toJson(timePoints))
    }
  }*/

}
