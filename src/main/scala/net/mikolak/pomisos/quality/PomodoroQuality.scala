package net.mikolak.pomisos.quality

import java.time.Instant

import shapeless.tag.@@

case class PomodoroQuality(timestamp: Instant, quality: Int @@ Quality)
