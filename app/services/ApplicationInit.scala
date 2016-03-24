package services

import javax.inject._

import akka.actor.{ActorRef, ActorSystem}
import model.DealDAO
import model.ExchangeCurrency
import play.Application
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import services.clients.ExchangeClientFactory
import services.clients.actors.MonitoringActor

import scala.concurrent.duration._
import scala.collection.JavaConversions._

@Singleton
class ApplicationInit @Inject()(application: Application, system: ActorSystem, clientFactory: ExchangeClientFactory, dealDao: DealDAO,
                                @Named("monitoring-actor") monitoringActor: ActorRef) {
/*
  val btcTradeComUaClient_UAH = clientFactory.createBtcTradeComUa(ExchangeCurrency.BTC, ExchangeCurrency.UAH)
  val btcEClient_USD = clientFactory.createBtcE(ExchangeCurrency.BTC, ExchangeCurrency.USD)
  system.scheduler.schedule(
    1.second, 100.seconds, monitoringActor, MonitoringActor.SaveDeals(btcTradeComUaClient_UAH))
  system.scheduler.schedule(
    2.second,  30.seconds, monitoringActor, MonitoringActor.SaveDeals(btcEClient_USD))
  system.scheduler.schedule(
    3.second,  1.minute, monitoringActor, MonitoringActor.SaveCandlesticks(btcEClient_USD, 1.minute))*/

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
        }
    }
  }

  private def getActorStartTime: FiniteDuration = {
    timeShift = timeShift + 1
    FiniteDuration(timeShift, SECONDS)
  }

}