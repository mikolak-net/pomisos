package net.mikolak.pomisos

package object quality {
  sealed trait Quality

  object Quality {

    val Min = 1
    val Max = 10

  }
}
