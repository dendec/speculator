package services.actors

import java.util.Date
import javax.inject.Inject

import akka.actor.Actor
import model._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import services.Calculator
import services.clients.ExchangeClient
import services.strategies.{TradingAction, TradingStrategy}

import scala.concurrent.duration.Duration

/**
  * Created by denis on 3/28/16.
  */
object TradingActor {
  case class MakeDecision(strategy: TradingStrategy, exchangeClient: ExchangeClient, duration: Duration)
}

class TradingActor @Inject() (tradingDecisionDAO: TradingDecisionDAO, candlestickDAO: CandlestickDAO, calculator: Calculator) extends Actor {
  override def receive = {
    case TradingActor.MakeDecision(strategy, client, duration) =>
      Logger.info(s"Making decision for ${client.name} with window duration $duration using ${strategy.getClass.getSimpleName}")
      val collectionName = Candlestick.getCollectionName(client, duration)
      candlestickDAO.get(collectionName).map { candlesticks =>
        val decision = strategy.makeDecision(candlesticks)
        val action = decision.action match {
          case TradingAction.BUY => 1
          case TradingAction.DO_NOTHING => 0
          case TradingAction.SELL => -1
        }
        val tradingDecision = new TradingDecision(new Date(), action, decision.fidelity, decision.price, client.exchange)
        if (!decision.action.equals(TradingAction.DO_NOTHING)) {
          Logger.warn(s"${decision.action} price: ${decision.price}")
          tradingDecisionDAO.add(tradingDecision, Candlestick.getCollectionName(client, duration) + "_" + strategy.getClass.getSimpleName)
        }
      }
  }
}
