package services

import java.util.Date
import javax.inject.Singleton

import model._
import org.joda.time.{DateTime, DateTimeZone, Period}
import play.api.Logger
import play.api.libs.json.{Format, Json}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration

/**
  * Created by denis on 3/16/16.
  */
case class TimePoint(date: Date, value: Double)

object TimePoint {
  implicit val format: Format[TimePoint] = Json.format[TimePoint]
}

//Moving Average Convergence/Divergence
case class MACD(macd: Seq[TimePoint], signal: Seq[TimePoint], diff: Seq[TimePoint])

object MACD {
  implicit val format: Format[MACD] = Json.format[MACD]
}

@Singleton
class Calculator {
  def getCandlesticks(duration: Duration)(deals: Seq[Deal], maybeStartDate: Option[Date] = None): Seq[Candlestick] = {
    val sortedDeals = deals.sortBy(_.date)
    val period = duration.toMillis
    val firstDeal = sortedDeals.head
    Logger.info(s"calculating candlesticks of ${firstDeal.exchange.name} with period $duration from ${firstDeal.date}")
    val firstDealDateTime = maybeStartDate match {
      case Some(startDate) => new DateTime(startDate.getTime, DateTimeZone.UTC)
      case None => new DateTime(firstDeal.date.getTime, DateTimeZone.UTC)
    }
    val firstDealStartDayDateTime = firstDealDateTime.withTimeAtStartOfDay()
    Logger.debug("firstDealDateTime " + firstDealDateTime)
    Logger.debug("firstDealStartDayDateTime " + firstDealStartDayDateTime)
    val periodsSinceStart = (firstDealDateTime.getMillis - firstDealStartDayDateTime.getMillis) / period
    Logger.debug("periodsSinceStart " + periodsSinceStart)
    val startDate = firstDealStartDayDateTime.withPeriodAdded(new Period(period), periodsSinceStart.toInt)
    Logger.debug("startDate " + startDate)
    val lastDeal = sortedDeals.last
    val range = lastDeal.date.getTime - firstDeal.date.getTime
    val candlestickNumber = range / duration.toMillis
    Logger.debug(s"range: $range, period: $period, candlesticks: $candlestickNumber")
    val groupedDeals = sortedDeals.groupBy(deal => (deal.date.getTime - startDate.getMillis) / period)
    val candlesticks: ArrayBuffer[Candlestick] = ArrayBuffer[Candlestick]()
    (0L to candlestickNumber).foreach { candlestickIndex =>
      val openDate = startDate.getMillis + period * candlestickIndex
      val closeDate = startDate.getMillis + period * (candlestickIndex + 1)
      groupedDeals.get(candlestickIndex) match {
        case Some(candlestickDeals) =>
          val max = candlestickDeals.maxBy(_.price)
          val min = candlestickDeals.minBy(_.price)
          val open = candlesticks.headOption.map(_.close).getOrElse(candlestickDeals.head.price)
          val close = candlestickDeals.last.price
          val volume = candlestickDeals.foldLeft(0.0)(_ + _.amount)
          candlesticks.+=:(new Candlestick(open, close, max.price, min.price, volume, new Date(closeDate)))
          Logger.debug(candlestickDeals.mkString("\n"))
        case None =>
          val prevCandlestick = candlesticks.head
          candlesticks.+=:(new Candlestick(prevCandlestick.close, prevCandlestick.close, prevCandlestick.close, prevCandlestick.close, 0.0, new Date(closeDate)))
      }
    }
    candlesticks.toSeq.reverse
  }

  def simpleMovingAverage(values: Seq[Double], period: Int): Seq[Double] =
    weightedMovingAverage(values, List.fill(period)(1.toDouble / period.toDouble))

  def linearMovingAverage(values: Seq[Double], period: Int): Seq[Double] =
    weightedMovingAverage(values, (1 to period).map(_.toDouble * 2 / (period + 1) / period).toSeq)

  def exponentialMovingAverage(values: Seq[Double], period: Int, alpha: Double): Seq[Double] =
    weightedMovingAverage(values, (0 until period).map(alpha * math.pow(1 - alpha, _)).toList.reverse)

  def exponentialMovingAverage(values: Seq[Double], period: Int): Seq[Double] =
    exponentialMovingAverage(values, period, 2.0 / (period.toDouble + 1.0))

  def weightedMovingAverage(values: Seq[Double], weights: Seq[Double]): Seq[Double] =
    List.fill(weights.size - 1)(0.0) ::: values.sliding(weights.size).toList.map(_.zip(weights).map {
      case (value, weight) => value * weight
    }.sum)

  def weightedMovingAverageByCandlesticks(candlesticks: Seq[Candlestick], weights: Seq[Double]): Seq[TimePoint] = {
    val sortedCandlestick = candlesticks.sortBy(_.date)
    weightedMovingAverage(sortedCandlestick.map(_.close), weights).zip(sortedCandlestick).map{
      case (value, candlestick) => TimePoint(candlestick.date, if (value == 0.0) sortedCandlestick.head.close else value)
    }
  }

  def simpleMovingAverageByCandlesticks(candlesticks: Seq[Candlestick], period: Int): Seq[TimePoint] = {
    weightedMovingAverageByCandlesticks(candlesticks, List.fill(period)(1.toDouble / period.toDouble))
  }

  def exponentialMovingAverageByCandlesticks(candlesticks: Seq[Candlestick], period: Int): Seq[TimePoint] = {
    exponentialMovingAverageByCandlesticks(candlesticks, period,  2.0 / (period.toDouble + 1.0)).toList.reverse
  }

  def exponentialMovingAverageByCandlesticks(candlesticks: Seq[Candlestick], period: Int, alpha: Double): Seq[TimePoint] = {
    weightedMovingAverageByCandlesticks(candlesticks, (0 until period).map(alpha * math.pow(1 - alpha, _)).toList.reverse)
  }

  def linearMovingAverageByCandlesticks(candlesticks: Seq[Candlestick], period: Int): Seq[TimePoint] = {
    weightedMovingAverageByCandlesticks(candlesticks, (1 to period).map(_.toDouble * 2 / (period + 1) / period).toSeq)
  }

  def getMACDByCandlesticks(candlesticks: Seq[Candlestick], shortPeriod: Int = 12, longPeriod: Int = 26, differencePeriod: Int = 9): MACD = {
    val short = linearMovingAverageByCandlesticks(candlesticks, shortPeriod)
    val long = linearMovingAverageByCandlesticks(candlesticks, longPeriod)
    val macd = (short zip long).map{
      case(shortTimePoint, longTimePoint) => TimePoint(shortTimePoint.date, shortTimePoint.value - longTimePoint.value)
    }
    val signal = (linearMovingAverage(macd.map(_.value), differencePeriod) zip macd.map(_.date)).map{
      case (value, date) => TimePoint(date, value)
    }
    val diff = (macd zip signal).map {
      case (macdTimePoint, signalTimePoint) => TimePoint(macdTimePoint.date, macdTimePoint.value - signalTimePoint.value)
    }
    MACD(macd, signal, diff)
  }

}
