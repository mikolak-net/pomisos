package net.mikolak.pomisos.graphics

import scalafx.scene.image.Image

/**
  * Wrapper type required to get around sfxml's macro rewriting and auto-injection (no, tagging doesn't work).
  */
case class MainIcon(image: Image)
