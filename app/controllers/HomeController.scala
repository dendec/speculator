package controllers

import javax.inject._

import model.{ExchangeCurrency, JsonExchangeDetails}
import play.api.libs.json.Json
import play.api.mvc._
import services.clients.ExchangeClientFactory

@Singleton
class HomeController @Inject() (clientFactory: ExchangeClientFactory) extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  def getExchanges = Action {
    import model.JsonExchangeDetails.format
    val result = clientFactory.exchanges.map{ex =>
      val baseCurrency = ExchangeCurrency.apply(ex.exchange.baseCurrency)
      val tradeCurrency = ExchangeCurrency.apply(ex.exchange.tradeCurrency)
      new JsonExchangeDetails(ex.exchange.name, s"${baseCurrency.toString}->${tradeCurrency.toString}",
        baseCurrency, tradeCurrency, ex.exchange.getCollectionName)
    }.groupBy(_.name)
    Ok(Json.toJson(result))
  }

}
