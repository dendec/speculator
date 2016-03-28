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
class DealsController @Inject()(dealDao: DealDAO) extends Controller with CrudController[Deal, JsonDeal, JsonDeal]{
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

}
