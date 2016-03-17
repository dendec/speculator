package services

import javax.inject._

import akka.actor.{ActorRef, ActorSystem}
import model.DealDAO
import model.ExchangeCurrency
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import services.clients.ExchangeClientFactory
import services.clients.actors.MonitoringActor

import scala.concurrent.duration._


@Singleton
class ApplicationInit @Inject()(system: ActorSystem, clientFactory: ExchangeClientFactory, dealDao: DealDAO,
                                @Named("monitoring-actor") monitoringActor: ActorRef) {

  val btcTradeComUaClient_UAH = clientFactory.createBtcTradeComUa(ExchangeCurrency.BTC, ExchangeCurrency.UAH)
  val btcEClient_USD = clientFactory.createBtcE(ExchangeCurrency.BTC, ExchangeCurrency.USD)
  system.scheduler.schedule(
    1.second, 100.seconds, monitoringActor, MonitoringActor.SaveDeals(btcTradeComUaClient_UAH))
  system.scheduler.schedule(
    2.second,  30.seconds, monitoringActor, MonitoringActor.SaveDeals(btcEClient_USD))
}