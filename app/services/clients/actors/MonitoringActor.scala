package services.clients.actors

import java.util.Date
import javax.inject.Inject

import akka.actor.Actor
import model.{CandlestickDAO, DealDAO}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import services.Calculator
import services.clients.ExchangeClient

import scala.concurrent.duration.Duration

/**
  * Created by denis on 3/11/16.
  */
object MonitoringActor {

  case class SaveDeals(exchangeClient: ExchangeClient)
  case class SaveCandlesticks(exchangeClient: ExchangeClient, duration: Duration)
  case class SaveAllCandlesticks(exchangeClient: ExchangeClient, duration: Duration)
}

class MonitoringActor @Inject() (dealDAO: DealDAO, candlestickDAO: CandlestickDAO, calculator: Calculator) extends Actor {

  def receive = {

    case MonitoringActor.SaveDeals(exchangeClient: ExchangeClient) =>
      Logger.info(s"saving deals of ${exchangeClient.exchange}")
      exchangeClient.getDeals.map { deals =>
        Logger.info(s"find ${deals.size} deals for ${exchangeClient.exchange}")
        dealDAO.addCollection(deals, exchangeClient.exchange.getCollectionName)
      }

    case MonitoringActor.SaveCandlesticks(exchangeClient: ExchangeClient, duration: Duration) =>
      Logger.info(s"save candlesticks of ${exchangeClient.exchange} duration=${duration.toMinutes} minutes")
      val collectionName = s"${exchangeClient.exchange.getCollectionName}_${duration.toMinutes}m"
      val currentDate = new Date()
      candlestickDAO.get(collectionName, currentDate.getTime - 5 * duration.toMillis, currentDate.getTime).map { savedCandlesticks =>
        if (savedCandlesticks.isEmpty) {
          self ! MonitoringActor.SaveAllCandlesticks(exchangeClient, duration)
        }
        else {
          val startDate = savedCandlesticks.maxBy(_.date).date
          dealDAO.get(exchangeClient.exchange.getCollectionName, startDate.getTime, currentDate.getTime).map { deals =>
            val candlesticks = calculator.getCandlesticks(duration)(deals)
            candlestickDAO.addCollection(candlesticks, collectionName)
          }
        }
      }

    case MonitoringActor.SaveAllCandlesticks(exchangeClient: ExchangeClient, duration: Duration) =>
      Logger.info(s"save all candlesticks of ${exchangeClient.exchange} duration=${duration.toMinutes} minutes")
      val collectionName = s"${exchangeClient.exchange.getCollectionName}_${duration.toMinutes}m"
      val currentDate = new Date()
      candlestickDAO.get(collectionName).map { savedCandlesticks =>
        if (savedCandlesticks.isEmpty)
          dealDAO.get(exchangeClient.exchange.getCollectionName).map { deals =>
            val candlesticks = calculator.getCandlesticks(duration)(deals)
            candlestickDAO.addCollection(candlesticks, collectionName)
          }
        else {
          val startDate = savedCandlesticks.maxBy(_.date).date
          dealDAO.get(exchangeClient.exchange.getCollectionName, startDate.getTime, currentDate.getTime).map { deals =>
            val candlesticks = calculator.getCandlesticks(duration)(deals)
            candlestickDAO.addCollection(candlesticks, collectionName)
          }
        }
      }
  }
}