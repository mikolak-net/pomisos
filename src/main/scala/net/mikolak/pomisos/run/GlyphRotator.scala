package net.mikolak.pomisos.run

import net.mikolak.pomisos.main.FontAwesomeGlyphs
import org.controlsfx.glyphfont.FontAwesome.Glyph

import javafx.scene.Node

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
