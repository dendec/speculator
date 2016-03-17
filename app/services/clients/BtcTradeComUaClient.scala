package services.clients

import java.text.SimpleDateFormat
import java.util.Locale

import model.Deal
import model.ExchangeCurrency._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.concurrent.Future

/**
  * Created by denis on 3/11/16.
  */
class BtcTradeComUaClient(val from: ExchangeCurrency, val to: ExchangeCurrency, ws: WSClient) extends ExchangeClient {

  private case class BtcTradeUADeal(amnt_base: String, amnt_trade: String, price: String, pub_date: String, user: String, `type`: String)
  private implicit val btcTradeUADealFormat = Json.format[BtcTradeUADeal]
  private def getDealsUrl = s"https://btc-trade.com.ua/api/deals/${from.toString}_${to.toString}"
  private val dateFormatter = new SimpleDateFormat("dd MMMM yyyy 'г.' hh:mm:ss", new Locale("ru"))

  override val name: String = ExchangeClient.BtcTradeComUaName

  override def getDeals: Future[Seq[Deal]] = {
    Logger.debug(s"Get: $getDealsUrl")
    ws.url(getDealsUrl).execute().map { result =>
      Logger.debug(s"Result: ${result.body}")
      val list = Json.parse(result.body).validate[List[BtcTradeUADeal]].get
      (list.filter(_.`type` == "sell") zip list.filter(_.`type` == "buy")).map { pair =>
        new Deal(dateFormatter.parse(pair._1.pub_date), pair._1.amnt_trade.toDouble, pair._1.price.toDouble, exchange)
      }
    }.recover{
      case ex =>
        Logger.error(ex.getMessage)
        List.empty
    }
  }
}