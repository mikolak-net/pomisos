package net.mikolak.pomisos.crud

import akka.actor.Status.Status
import net.mikolak.pomisos.prefs.Command.{IdKey, WithId}

trait Dao[T <: Product with Serializable] {

  def getAll(): List[T]
  def save(e: T): T
  def saveWith(transform: T => T): T
  def removeAll(): Unit

}

trait SingletonDao[T <: Product with Serializable] extends Dao[T] {
  def get(): T
  final def getAll() = List(get())

}

abstract class MultiDao[T <: Product with Serializable: Idable] extends Dao[T] {
  def get(id: IdKey): Option[T]
  def remove(id: IdKey*): Unit
}

trait Idable[T] {
  def idsOf(t: T): Seq[IdKey]
}
