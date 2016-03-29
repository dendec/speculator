package services.strategies

import model.Candlestick
import services.strategies.TradingAction.TradingAction

/**
  * Created by denis on 3/28/16.
  */
trait TradingStrategy {
  def makeDecision(candlesticks: Seq[Candlestick]): Decision
}

case class Decision(action: TradingAction, fidelity: Double, price: Double)

object TradingAction extends Enumeration {
  type TradingAction = Value
  val BUY, SELL, DO_NOTHING = Value
}