package model

import java.util.Date

import play.api.libs.json.{Json, Format}

/**
  * Created by denis on 3/22/16.
  */
case class TimePoint(date: Date, value: Double)

object TimePoint {
  implicit val format: Format[TimePoint] = Json.format[TimePoint]
  type NamedTimeSeries = Map[String, Seq[TimePoint]]
}
