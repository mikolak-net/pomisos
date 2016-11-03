package net.mikolak.pomisos.graphics

import javafx.scene.Node

import org.controlsfx.glyphfont.FontAwesome.Glyph

import scalafx.beans.property.ObjectProperty

class GlyphRotators(glyphs: FontAwesomeGlyphs) {

  def apply(glyphList: Glyph*) = new GlyphRotator(glyphList.toList.map(glyphs.apply))

}

class GlyphRotator private[graphics](nodes: List[Node]) {
  require(nodes.nonEmpty)

  private var pointer = 0

  private def current() = nodes(pointer)

  val value = ObjectProperty[Node](current())

  def rotate(): Unit = {
    pointer = (pointer + 1) % nodes.size
    value.value = current()
  }

}
