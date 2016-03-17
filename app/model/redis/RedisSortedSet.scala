package model.redis

import akka.actor.ActorRefFactory
import model.Identifiable
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import redis.RedisClient
import redis.api.Limit

import scala.concurrent.Future

/**
  * Created by denis on 13.02.16.
  */
trait RedisSortedSet[T <: Identifiable[Double]] extends RedisPersistent[T] {

  implicit val akkaSystem: ActorRefFactory
  val client: RedisClient = RedisClient()

  def addCollection(values: Seq[T], k: String): Future[Long] = {
    Logger.info(s"add ${values.size} entities to $k")
    val v = values.map { value =>
      (value.getId, value)
    }
    client.zadd[T](k, v: _*)
  }

  def add(value: T, k: String): Future[Long] =
    addCollection(Seq(value), k)

  def get(k: String): Future[Seq[T]] = {
    get(k, 0L, -1L)
  }

  def get(k: String, from: Long, to: Long): Future[Seq[T]] = {
    Logger.info(s"get from $k: [$from, $to]")
    client.zrangebyscoreWithscores[T](k, Limit(from), Limit(to)).map { results =>
      results.map{
        case (entity, score) =>
          entity
      }
    }
  }

  def get(k: String, id: Long): Future[Option[T]] = {
    get(k, id, id).map(_.headOption)
  }

  def getFirst(k: String): Future[Option[T]] = {
    client.zrange[T](k, 0, 0).map(_.headOption)
  }

  def getLast(k: String): Future[Option[T]] = {
    client.zrange[T](k, -1, -1).map(_.headOption)
  }

  def isMember(value: T, k: String): Future[Boolean] = {
    Logger.info(s"check existence $value in $k")
    client.zrevrank[T](k, value).map(_.isDefined)
  }

  def delete(value: Seq[T], k: String) = {
    Logger.info(s"delete $value from $k")
    client.zrem[T](k, value: _*)
  }

  def delete(value: T, k: String): Future[Long] = {
    delete(Seq(value), k)
  }

  def update(value: T, id: Long, k: String): Future[Long] = {
    Logger.info(s"update $value from $k")
    client.zrangeWithscores[T](k, id, id).flatMap { entities =>
      entities.head match {
        case (oldValue, score) =>
          client.zrem[T](k, oldValue)
          client.zadd[T](k, (score, value))
        case _ =>
          Future(-1L)
      }
    }
  }

}
