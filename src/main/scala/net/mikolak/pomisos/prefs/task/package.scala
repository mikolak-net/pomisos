package net.mikolak.pomisos.prefs

import net.mikolak.pomisos.data.IdOf

package object task {
  case class Board(id: IdOf[Board], name: String)

  case class CardList(id: IdOf[CardList], name: String)

  case class Card(id: IdOf[Card], name: String, idList: IdOf[CardList])

}
