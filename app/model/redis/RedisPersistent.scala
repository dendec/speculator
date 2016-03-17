package model.redis

import redis.ByteStringFormatter

/**
  * Created by denis on 2/10/16.
  */
trait RedisPersistent[T] {

  implicit val byteStringFormatter: ByteStringFormatter[T]

}
