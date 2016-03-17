package services.clients.actors

import javax.inject.Inject

import akka.actor.Actor
import model.DealDAO
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import services.clients.ExchangeClient
import services.clients.actors.MonitoringActor.SaveDeals

/**
  * Created by denis on 3/11/16.
  */
object MonitoringActor {

  case class SaveDeals(exchangeClient: ExchangeClient)

}

class MonitoringActor @Inject() (dealDAO: DealDAO) extends Actor {

  def receive = {

    case SaveDeals(exchangeClient: ExchangeClient) =>
      Logger.info(s"saving deals of ${exchangeClient.exchange}")
      exchangeClient.getDeals.map { deals =>
        Logger.info(s"find ${deals.size} deals for ${exchangeClient.exchange}")
        dealDAO.addCollection(deals, exchangeClient.exchange.getCollectionName)
      }
  }
}