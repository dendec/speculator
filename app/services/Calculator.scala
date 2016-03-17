package services

import javax.inject.Singleton

import model.{Candlestick, Deal}
import org.joda.time.{DateTimeZone, Period, DateTime}
import play.api.Logger

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration

/**
  * Created by denis on 3/16/16.
  */
@Singleton
class Calculator {
  def getCandlesticks(duration: Duration)(deals: Seq[Deal]): Seq[Candlestick] = {
    val sortedDeals = deals.sortBy(_.date)
    val period = duration.toMillis
    val firstDeal = sortedDeals.head
    val firstDealDateTime = new DateTime(firstDeal.date.getTime, DateTimeZone.UTC)
    val firstDealStartDayDateTime = firstDealDateTime.withTimeAtStartOfDay()
    Logger.debug("firstDealDateTime " + firstDealDateTime)
    Logger.debug("firstDealStartDayDateTime " + firstDealStartDayDateTime)
    val periodsSinceStart = (firstDealDateTime.getMillis - firstDealStartDayDateTime.getMillis) / period
    Logger.debug("periodsSinceStart " + periodsSinceStart)
    val startDate = firstDealStartDayDateTime.withPeriodAdded(new Period(period), periodsSinceStart.toInt)
    Logger.debug("startDate " + startDate)
    val lastDeal = sortedDeals.last
    val range = lastDeal.date.getTime - firstDeal.date.getTime
    val candlestickNumber = range/duration.toMillis
    Logger.debug(s"range: $range, period: $period, candlesticks: $candlestickNumber")
    val groupedDeals = sortedDeals.groupBy(deal => (deal.date.getTime - startDate.getMillis) / period)
    val candlesticks: ArrayBuffer[Candlestick] = ArrayBuffer[Candlestick]()
    (0L to candlestickNumber).foreach{ candlestickIndex =>
      val openDate = startDate.getMillis + period * candlestickIndex
      val closeDate = startDate.getMillis + period * (candlestickIndex + 1)
      groupedDeals.get(candlestickIndex) match {
        case Some(candlestickDeals) =>
          val max = candlestickDeals.maxBy(_.price)
          val min = candlestickDeals.minBy(_.price)
          val open = candlesticks.headOption.map(_.close).getOrElse(candlestickDeals.head.price)
          val close = candlestickDeals.last.price
          val volume = candlestickDeals.foldLeft(0.0)(_ + _.amount)
          candlesticks.+=:(Candlestick(open, close, max.price, min.price, volume, openDate))
          Logger.debug(candlestickDeals.mkString("\n"))
        case None =>
          val prevCandlestick = candlesticks.head
          candlesticks.+=:(Candlestick(prevCandlestick.close, prevCandlestick.close,prevCandlestick.close, prevCandlestick.close, 0.0, openDate))
      }
      Logger.debug(candlesticks.toString())
    }
    candlesticks.toSeq.reverse
  }

  def simpleMovingAverage(values: Seq[Double], period: Int): Seq[Double] =
    weightedMovingAverage(values, List.fill(period)(1.toDouble / period.toDouble))

  def linearMovingAverage(values: Seq[Double], period: Int): Seq[Double] =
    weightedMovingAverage(values, (1 to period).map(_.toDouble * 2 / (period + 1) / period).toSeq)

  def exponentialMovingAverage(values: Seq[Double], period: Int, alpha: Double): Seq[Double] =
    weightedMovingAverage(values, (0 until period).map(alpha * math.pow(1 - alpha, _)).toList.reverse)

  def weightedMovingAverage(values: Seq[Double], weights: Seq[Double]): Seq[Double] =
    List.fill(weights.size - 1)(0.0) ::: values.sliding(weights.size).toList.map(_.zip(weights).map {
      case (value, weight) => value * weight
    }.sum)

}
