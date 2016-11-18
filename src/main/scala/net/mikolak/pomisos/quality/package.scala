package net.mikolak.pomisos

package object quality {
  sealed trait Quality

  val MinQuality = 0
  val MaxQuality = 10
}
