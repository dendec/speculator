package model

/**
  * Created by denis on 05.03.16.
  */
trait Identifiable[T] {
  def getId: T
  def setId(id: T)
}
