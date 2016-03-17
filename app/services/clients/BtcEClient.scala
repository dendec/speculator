package services.clients

import java.util.Date

import model.Deal
import model.ExchangeCurrency.ExchangeCurrency
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

/**
  * Created by denis on 3/14/16.
  */
class BtcEClient(val from: ExchangeCurrency, val to: ExchangeCurrency, ws: WSClient) extends ExchangeClient {

  private case class BtcEDeal(date: Long, price: Double, amount: Double, tid: Int, price_currency: String, item: String, trade_type: String)
  private implicit val btcEDealFormat = Json.format[BtcEDeal]
  private def getDealsUrl = s"https://btc-e.com/api/2/${from.toString.toLowerCase}_${to.toString.toLowerCase}/trades"

  override def getDeals: Future[Seq[Deal]] = {
    Logger.debug(s"Get: $getDealsUrl")
    ws.url(getDealsUrl).execute().map { result =>
      Logger.debug(s"Result: ${result.body}")
      val list = Json.parse(result.body).validate[List[BtcEDeal]].get
      list.map { d =>
        new Deal(new Date(d.date * 1000), d.amount, d.price, exchange)
      }

    }.recover{
      case ex =>
        Logger.error(ex.getMessage)
        List.empty
    }
  }

  override val name: String = ExchangeClient.BtcEName
}
