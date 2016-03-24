import com.google.inject.AbstractModule
import model.{CandlestickDAO, DealDAO}
import play.api.libs.concurrent.AkkaGuiceSupport
import services.{Calculator, ApplicationInit}
import services.clients.ExchangeClientFactory
import services.clients.actors.MonitoringActor

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
    bind(classOf[DealDAO]).asEagerSingleton()
    bind(classOf[CandlestickDAO]).asEagerSingleton()
    bind(classOf[ApplicationInit]).asEagerSingleton()
    bind(classOf[ExchangeClientFactory]).asEagerSingleton()
    bind(classOf[Calculator])
  }

}
