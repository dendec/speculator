package services.clients

import javax.inject.{Inject, Singleton}

import model.ExchangeCurrency.ExchangeCurrency
import model.{Deal, ExchangeDetails}
import play.api.libs.ws.WSClient

import scala.concurrent.Future

/**
  * Created by denis on 3/10/16.
  */

trait ExchangeClient {
  val from: ExchangeCurrency
  val to: ExchangeCurrency
  val name: String
  lazy val exchange: ExchangeDetails = new ExchangeDetails(name, from.id, to.id)

  def getDeals: Future[Seq[Deal]]
}

object ExchangeClient {
  val BtcTradeComUaName = "btc-trade.com.ua"
  val BtcEName = "btc-e.com"
}

@Singleton
class ExchangeClientFactory @Inject()(ws: WSClient) {

  var exchanges: Seq[ExchangeClient] = Seq.empty[ExchangeClient]

  private def createExchange(name: String, from: ExchangeCurrency, to: ExchangeCurrency) = {
    exchanges.find(ex =>
      ex.exchange.name.equals(name) && ex.from.equals(from) && ex.to.equals(to)
    ).orElse {
      val maybeClient = name match {
        case ExchangeClient.BtcEName => Some(new BtcEClient(from, to, ws))
        case ExchangeClient.BtcTradeComUaName => Some(new BtcTradeComUaClient(from, to, ws))
        case _ => None
      }
      maybeClient.map { client =>
        exchanges = exchanges :+ client
        client
      }
    }
  }

  def createBtcTradeComUa(from: ExchangeCurrency, to: ExchangeCurrency): ExchangeClient =
    createExchange(ExchangeClient.BtcTradeComUaName, from, to).get

  def createBtcE(from: ExchangeCurrency, to: ExchangeCurrency): ExchangeClient =
    createExchange(ExchangeClient.BtcEName, from, to).get

}