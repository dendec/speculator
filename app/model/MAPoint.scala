package model

import play.api.libs.json.{Json, Format}

/**
  * Created by denis on 3/17/16.
  */
case class MAPoint(date: Long, ma7: Double, ma15: Double, ma30: Double)

object MAPoint {
  implicit val format: Format[MAPoint] = Json.format[MAPoint]
}