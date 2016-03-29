package services

import javax.inject._

import akka.actor.{ActorRef, ActorSystem}
import model.{DealDAO, ExchangeCurrency}
import play.Application
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import services.clients.ExchangeClientFactory
import services.actors.{MonitoringActor, TradingActor}
import services.strategies.TradingStrategy

import scala.collection.JavaConversions._
import scala.concurrent.duration._

@Singleton
class ApplicationInit @Inject()(application: Application, system: ActorSystem, clientFactory: ExchangeClientFactory, dealDao: DealDAO,
                                @Named("monitoring-actor") monitoringActor: ActorRef, @Named("trading-actor") tradingActor: ActorRef,
                                @Named("macd") strategy: TradingStrategy) {

  private val candlestickPeriods = application.configuration().getLongList("monitoring.candlestickPeriods")
  private var timeShift: Int = 0
  application.configuration().getConfigList("monitoring.exchanges").foreach{ exchangeConfiguration =>
    val name = exchangeConfiguration.getString("name")
    val from = exchangeConfiguration.getString("from")
    val to = exchangeConfiguration.getString("to")
    val period = exchangeConfiguration.getLong("updatePeriod")
    clientFactory.createExchange(name, ExchangeCurrency.withName(from), ExchangeCurrency.withName(to)).foreach {
      exchange =>
        system.scheduler.schedule(
          getActorStartTime, FiniteDuration(period, SECONDS), monitoringActor, MonitoringActor.SaveDeals(exchange))
        candlestickPeriods.foreach{ candlestickPeriod =>
          val candlestickDuration = FiniteDuration(candlestickPeriod, MINUTES)
          system.scheduler.schedule(
            getActorStartTime, candlestickDuration, monitoringActor, MonitoringActor.SaveCandlesticks(exchange, candlestickDuration))
          system.scheduler.schedule(getActorStartTime, candlestickDuration / 2, tradingActor,
            TradingActor.MakeDecision(strategy, exchange, candlestickDuration))
        }
    }
  }

  private def getActorStartTime: FiniteDuration = {
    timeShift = timeShift + 1
    FiniteDuration(timeShift, SECONDS)
  }

}