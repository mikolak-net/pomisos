package net.mikolak.pomisos.crud

trait Dao[T <: Product with Serializable] {

  def get(): T
  def save(e: T): T
  def saveWith(transform: T => T): T

}
