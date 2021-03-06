import com.google.inject.AbstractModule
import com.google.inject.name.Names
import model.{CandlestickDAO, DealDAO}
import play.api.libs.concurrent.AkkaGuiceSupport
import services.strategies.{MACDTradingStrategy, TradingStrategy}
import services.{Calculator, ApplicationInit}
import services.clients.ExchangeClientFactory
import services.actors.{TradingActor, MonitoringActor}

/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.

 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
class Module extends AbstractModule with AkkaGuiceSupport {

  override def configure() = {
    bindActor[MonitoringActor]("monitoring-actor")
    bindActor[TradingActor]("trading-actor")
    bind(classOf[DealDAO]).asEagerSingleton()
    bind(classOf[CandlestickDAO]).asEagerSingleton()
    bind(classOf[ApplicationInit]).asEagerSingleton()
    bind(classOf[ExchangeClientFactory]).asEagerSingleton()
    bind(classOf[Calculator])
    bind(classOf[TradingStrategy]).annotatedWith(Names.named("macd"))
      .to(classOf[MACDTradingStrategy])
  }

}
