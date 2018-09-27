package net.mikolak.pomisos.dependencies

import com.softwaremill.macwire._
import net.mikolak.pomisos.graphics.{FontAwesomeGlyphs, GlyphRotators, MainIcon}
import net.mikolak.pomisos.ui.CommitmentColorCalculator
import net.mikolak.pomisos.utils.Notifications
import scalafx.scene.image.Image

trait UiModule {
  lazy val icon: MainIcon = MainIcon(new Image(this.getClass.getResource("/icon.png").toExternalForm))

  lazy val commitmentColorCalculator = wire[CommitmentColorCalculator]

  lazy val notifications = wire[Notifications]

  lazy val glyphs = wire[FontAwesomeGlyphs]

  lazy val glyphRotators = wire[GlyphRotators]
}


