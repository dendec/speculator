package controllers

import javax.inject.Inject

import model.{JsonCandlestick, CandlestickDAO}
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import services.{TimePoint, Calculator}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
  * Created by denis on 3/28/16.
  */
class AnalysisController @Inject() (candlestickDAO: CandlestickDAO, calculator: Calculator) extends Controller {


  def getSMA(key: String, from: Option[Long], to: Option[Long]) = Action.async {
    candlestickDAO.get(key, from.getOrElse(Long.MinValue), to.getOrElse(Long.MaxValue)).map { candlesticks =>
      val price = candlesticks.map(candlestick => TimePoint(candlestick.date, candlestick.close))
      val sma7 = calculator.simpleMovingAverageByCandlesticks(candlesticks, 7)
      val sma15 = calculator.simpleMovingAverageByCandlesticks(candlesticks, 15)
      val sma30 = calculator.simpleMovingAverageByCandlesticks(candlesticks, 30)
      Ok(Json.toJson(Map("price" -> price, "sma7" -> sma7, "sma15" -> sma15, "sma30" -> sma30)))
    }
  }

  def getLMA(key: String, from: Option[Long], to: Option[Long]) = Action.async {
    candlestickDAO.get(key, from.getOrElse(Long.MinValue), to.getOrElse(Long.MaxValue)).map { candlesticks =>
      val price = candlesticks.map(candlestick => TimePoint(candlestick.date, candlestick.close))
      val lma7 = calculator.linearMovingAverageByCandlesticks(candlesticks, 7)
      val lma15 = calculator.linearMovingAverageByCandlesticks(candlesticks, 15)
      val lma30 = calculator.linearMovingAverageByCandlesticks(candlesticks, 30)
      Ok(Json.toJson(Map("price" -> price, "lma7" -> lma7, "ema15" -> lma15, "ema30" -> lma30)))
    }
  }

  def getMACD(key: String, from: Option[Long], to: Option[Long]) = Action.async {
    candlestickDAO.get(key, from.getOrElse(Long.MinValue), to.getOrElse(Long.MaxValue)).map { candlesticks =>
      Ok(Json.toJson(calculator.getMACDByCandlesticks(candlesticks)))
    }
  }

  def getCandlesticks(key: String, from: Option[Long], to: Option[Long]) = Action.async {
    candlestickDAO.get(key, from.getOrElse(Long.MinValue), to.getOrElse(Long.MaxValue)).map {candlesticks =>
      Ok(Json.toJson(candlesticks.map(JsonCandlestick.convert)))
    }
  }

}
