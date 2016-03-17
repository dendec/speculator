package controllers

import model.Identifiable
import model.redis.RedisSortedSet
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{Format, JsError, JsSuccess, Json}
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.Future

/**
  * Created by denis on 24.02.16.
  */
trait Converter[Type, InputType, OutputType] {
  def convertIn(input: InputType): Type
  def convertOut(input: Type): OutputType
}

trait CrudController[Type <: Identifiable[Double], InputType, OutputType] {

  val persistence: RedisSortedSet[Type]

  val converter: Converter[Type, InputType, OutputType]

  implicit val inputFormat: Format[InputType]
  implicit val outputFormat: Format[OutputType]

  def add(key: String) = Action.async(parse.json) { request =>
    val inputEntity: InputType = request.body.as[InputType]
    val entity = converter.convertIn(inputEntity)
    persistence.add(entity, key).map { result =>
      Ok(result.toString)
    }
  }

  def addFromArray(key: String) = Action.async(parse.json) { request =>
    val inputEntities: Seq[InputType] = request.body.as[Seq[InputType]]
    val entities = inputEntities.map(converter.convertIn)
    persistence.addCollection(entities, key).map { result =>
      Ok(result.toString)
    }
  }

  def get(key: String, from: Option[Long], to: Option[Long]) = Action.async {
    persistence.get(key, from.getOrElse(Long.MinValue), to.getOrElse(Long.MaxValue)).map { entities =>
      val result = entities.map(converter.convertOut)
      Ok(Json.toJson(result)).withHeaders(("Cache-Control", "no-store"))
    }
  }

  def deleteById(key: String, id: Long) = Action.async {
    persistence.get(key, id).flatMap { leads =>
      leads.headOption match {
        case Some(lead) =>
          persistence.delete(lead, key).map { result =>
            Ok(result.toString)
          }.recover { case ex =>
            InternalServerError(ex.toString)
          }
        case None =>
          Future(BadRequest("Can not delete entity that not exist"))
      }
    }
  }

  def delete(key: String) = Action.async { request =>
    request.body.asJson match {
      case Some(json) => json.validate[Seq[InputType]] match {
        case JsSuccess(entitiesToDelete, _) =>
          persistence.delete(entitiesToDelete.map(converter.convertIn), key).map { result =>
            Ok(result.toString)
          }.recover { case ex =>
            InternalServerError(ex.toString)
          }
        case JsError(errors) =>
          Future(BadRequest(Json.toJson(errors.toString)))
      }
      case None =>
        Future(BadRequest("Expect to receive JSON array of entities to delete"))
    }
  }

  def update(key: String, id: Long) = Action.async(parse.json) { request =>
    val inputEntity: InputType = request.body.as[InputType]
    val entity = converter.convertIn(inputEntity)
    persistence.update(entity, id, key).map { result =>
      Ok(result.toString)
    }
  }

  def exportAsJson(key: String) = Action.async {
    val filename = s"${key}_${new java.util.Date().getTime}.json"
    persistence.get(key, 0L, -1L).map { entities =>
      Ok(Json.toJson(entities.map(converter.convertOut))).withHeaders(("Cache-Control", "no-store")).withHeaders(
        ("Content-Disposition", "attachment; filename=" + filename)
      )
    }
  }

  def importFromJson(key: String) = Action.async(parse.multipartFormData) { request =>
    request.body.file("file").map { file =>
      val fileContent = scala.io.Source.fromFile(file.ref.file).mkString
      Json.parse(fileContent).validate[Seq[InputType]] match {
        case JsSuccess(result, jsPath) =>
          persistence.addCollection(result.map(converter.convertIn), key).map { result =>
            Ok(result.toString)
          }
        case JsError(errors) =>
          Future(BadRequest(Json.toJson(errors.toString)))
      }
    }.getOrElse {
      Future(BadRequest(Json.toJson("missing file")))
    }
  }

}
