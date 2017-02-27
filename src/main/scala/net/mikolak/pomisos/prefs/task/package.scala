package net.mikolak.pomisos.prefs

import net.mikolak.pomisos.data.{IdOf, WithGenericId}

package object task {
  case class Board(id: IdOf[Board], name: String) extends WithGenericId[Board]

  case class CardList(id: IdOf[CardList], name: String) extends WithGenericId[CardList]

  case class Card(id: IdOf[Card], name: String, idList: IdOf[CardList]) extends WithGenericId[Card]

}
