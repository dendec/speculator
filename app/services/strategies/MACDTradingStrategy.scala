package services.strategies

import javax.inject.Inject

import model.Candlestick
import services.Calculator
import services.strategies.TradingAction._

/**
  * Created by denis on 3/28/16.
  */
class MACDTradingStrategy @Inject() (calculator: Calculator) extends TradingStrategy {

  val thresholdFactor = 0.05

  override def makeDecision(candlesticks: Seq[Candlestick]): Decision = {
    val diffSequence = calculator.getMACDByCandlesticks(candlesticks).diff
    val buyThreshold = diffSequence.map(_.value).max * thresholdFactor
    val sellThreshold = diffSequence.map(_.value).min * thresholdFactor
    val previousDiff = diffSequence.takeRight(2).head
    val lastDiff = diffSequence.last
    if ((previousDiff.value > sellThreshold) && (lastDiff.value < sellThreshold)) {
      Decision(SELL, -lastDiff.value, candlesticks.last.close)
    } else {
      if ((previousDiff.value < buyThreshold) && (lastDiff.value > buyThreshold)) {
        Decision(BUY, lastDiff.value, candlesticks.last.close)
      } else {
        Decision(DO_NOTHING, math.abs(lastDiff.value), candlesticks.last.close)
      }
    }
  }
}
