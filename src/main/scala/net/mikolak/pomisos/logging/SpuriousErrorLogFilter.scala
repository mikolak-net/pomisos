package net.mikolak.pomisos.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply

/**
  * Filter for removing spurious messages from various dependencies/subsystems.
  *
  * Implemented In-Scala instead of using matchers to allow for better reuse.
  */
class SpuriousErrorLogFilter extends Filter[ILoggingEvent] {

  val SpuriousStrings = Set("smile.",
                            "scanning through all elements without using an index for Traversal",
                            "MaxDirectMemorySize JVM option is not set")

  override def decide(event: ILoggingEvent): FilterReply =
    if (SpuriousStrings.exists(event.getFormattedMessage.contains)) {
      FilterReply.DENY
    } else {
      FilterReply.NEUTRAL
    }
}
