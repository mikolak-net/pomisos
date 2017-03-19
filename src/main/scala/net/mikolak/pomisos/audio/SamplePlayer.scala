package net.mikolak.pomisos.audio

import javax.sound.sampled._

//TODO: refactor to actor
class SamplePlayer(sampleAtStr: String) {

  private lazy val sampleAt = getClass.getResource(sampleAtStr)

  lazy val sample = {
    val audioIn = AudioSystem.getAudioInputStream(sampleAt)
    val clip    = AudioSystem.getClip
    clip.open(audioIn)
    val endPos = clip.getFrameLength - 1
    clip.setLoopPoints(0, endPos)
    clip.setFramePosition(endPos)
    clip
  }

  def play() = {
    sample.loop(1)
    sample.start()
    sample.flush()
  }

  def stop() = {
    sample.stop()
    sample.drain()
  }

}
