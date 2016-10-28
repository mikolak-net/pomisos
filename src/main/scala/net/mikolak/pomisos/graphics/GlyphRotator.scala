package net.mikolak.pomisos.graphics

import javafx.scene.Node

import org.controlsfx.glyphfont.FontAwesome.Glyph

class GlyphRotators(glyphs: FontAwesomeGlyphs) {

  def apply(glyphList: Glyph*) = GlyphRotator(glyphList.toList.map(glyphs.apply))

}

case class GlyphRotator(nodes: List[Node]) {
  require(nodes.nonEmpty)

  var pointer = 0

  def current() = nodes(pointer)

  def next() = {
    pointer = (pointer + 1) % nodes.size
    current()
  }

}
