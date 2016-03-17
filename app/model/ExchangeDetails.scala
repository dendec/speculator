package model

import model.ExchangeCurrency.ExchangeCurrency
import org.msgpack.annotation.Message
import play.api.libs.json._

/**
  * Created by denis on 3/11/16.
  */
@Message
class ExchangeDetails(var name: String, var baseCurrency: Int, var tradeCurrency: Int){

  def this() = this("", 0, 0)

  def getCollectionName = s"${name}_${ExchangeCurrency.apply(baseCurrency)}_${ExchangeCurrency.apply(tradeCurrency)}"

  override def toString =
    s"ExchangeDetails($name, ${ExchangeCurrency.apply(baseCurrency)}," +
      s" ${ExchangeCurrency.apply(tradeCurrency)})"
}

object ExchangeCurrency extends Enumeration {
  type ExchangeCurrency = Value
  val UAH, USD, BTC, LTC, NVC, DRK, VTC, PPC, HIRO, DOGE, XMR, CLR, RMS = Value
  implicit val format = new Format[ExchangeCurrency] {
    def reads(json: JsValue) = JsSuccess(ExchangeCurrency.withName(json.as[String]))
    def writes(currency: ExchangeCurrency) = JsString(currency.toString)
  }
}

case class JsonExchangeDetails(name: String, currencies: String, baseCurrency: ExchangeCurrency, tradeCurrency: ExchangeCurrency, collection: String)

object JsonExchangeDetails {
  implicit val format: Format[JsonExchangeDetails] = Json.format[JsonExchangeDetails]
}
