package net.mikolak.pomisos.audio

import javax.sound.sampled._

class SamplePlayer(sampleAtStr: String) {

  private lazy val sampleAt = getClass.getResource(sampleAtStr)

  private var firstLoop = false

  lazy val sample = {
    val audioIn = AudioSystem.getAudioInputStream(sampleAt)
    val clip = AudioSystem.getClip
    val endPos = clip.getFrameLength-1
    clip.open(audioIn)
    clip.setFramePosition(endPos)
    clip.setLoopPoints(0, endPos)
    clip
  }

  def play() = {
    sample.start()
    if(!firstLoop) {
      firstLoop = true
    } else {
      sample.loop(1)
    }
  }

}
