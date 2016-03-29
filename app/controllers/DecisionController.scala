package controllers

import javax.inject.{Inject, Singleton}

import model._
import model.redis.RedisSortedSet
import play.api.libs.json.Format
import play.api.mvc.Controller

/**
  * Created by denis on 3/11/16.
  */
@Singleton
class DecisionController @Inject()(tradingDecisionDao: TradingDecisionDAO) extends Controller with CrudController[TradingDecision, JsonTradingDecision, JsonTradingDecision]{
  override val persistence: RedisSortedSet[TradingDecision] = tradingDecisionDao
  override val converter: Converter[TradingDecision, JsonTradingDecision, JsonTradingDecision] = new Converter[TradingDecision, JsonTradingDecision, JsonTradingDecision] {
    override def convertIn(input: JsonTradingDecision): TradingDecision = {
      val exchange = new ExchangeDetails(input.exchange.name, input.exchange.baseCurrency.id, input.exchange.tradeCurrency.id)
      val action = input.action match {
        case "SELL" => 1
        case "BUY" => -1
        case _ => 0
      }
      new TradingDecision(input.date, action, input.fidelity, input.price, exchange)
    }

    override def convertOut(input: TradingDecision): JsonTradingDecision = {
      val baseCurrency = ExchangeCurrency.apply(input.exchange.baseCurrency)
      val tradeCurrency = ExchangeCurrency.apply(input.exchange.tradeCurrency)
      val exchange = new JsonExchangeDetails(input.exchange.name, s"${baseCurrency.toString}->${tradeCurrency.toString}",
        baseCurrency, tradeCurrency, input.exchange.getCollectionName)
      val action = input.action match {
        case 1 => "SELL"
        case -1 => "BUY"
        case _ => "DO_NOTHING"
      }
      JsonTradingDecision(input.date, action, input.fidelity, input.price, exchange)
    }
  }

  override implicit val inputFormat: Format[JsonTradingDecision] = JsonTradingDecision.format
  override implicit val outputFormat: Format[JsonTradingDecision] = JsonTradingDecision.format
}
