package net.mikolak.pomisos.crud

import net.mikolak.pomisos.data.DbIdKey

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

abstract class MultiDao[T <: Product with Serializable: DbIdable] extends Dao[T] {
  def get(id: DbIdKey): Option[T]
  def remove(id: DbIdKey*): Unit
  def saveWith(transform: T => T)(id: DbIdKey): Option[T] = get(id).map(transform).map(save)
}

trait DbIdable[T] {
  def idsOf(t: T): Seq[DbIdKey]
}

/**
  * No generic OrientDBDao implementation due to the current problem with scala-gremlin's toCC macro-based implementation,
  * which can't derive class information from generic parameters.
  */
