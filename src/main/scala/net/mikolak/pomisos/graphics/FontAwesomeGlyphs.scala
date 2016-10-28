package net.mikolak.pomisos.graphics

import org.controlsfx.glyphfont.FontAwesome.Glyph
import org.controlsfx.glyphfont.GlyphFontRegistry

class FontAwesomeGlyphs {

  lazy val fontAwesome = GlyphFontRegistry.font("FontAwesome")

  def apply(symbol: Glyph) = fontAwesome.create(symbol)

}
