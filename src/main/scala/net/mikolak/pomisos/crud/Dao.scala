package net.mikolak.pomisos.crud

import net.mikolak.pomisos.prefs.Command.{IdKey, WithId}

trait Dao[T <: Product with Serializable] {

  def getAll(): List[T]
  def save(e: T): T
  def removeAll(): Unit

}

trait SingletonDao[T <: Product with Serializable] extends Dao[T] {
  def get(): T
  final def getAll()                 = List(get())
  def saveWith(transform: T => T): T = save(transform(get()))
}

abstract class MultiDao[T <: Product with Serializable: Idable] extends Dao[T] {
  def get(id: IdKey): Option[T]
  def remove(id: IdKey*): Unit
  def saveWith(transform: T => T)(id: IdKey): Option[T] = get(id).map(transform).map(save)
}

trait Idable[T] {
  def idsOf(t: T): Seq[IdKey]
}

/**
  * No generic OrientDBDao implementation due to the current problem with scala-gremlin's toCC macro-based implementation,
  * which can't derive class information from generic parameters.
  */
