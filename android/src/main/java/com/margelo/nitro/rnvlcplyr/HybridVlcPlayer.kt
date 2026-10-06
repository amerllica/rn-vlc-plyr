package com.margelo.nitro.rnvlcplyr

import androidx.annotation.Keep
import com.facebook.proguard.annotations.DoNotStrip
import com.margelo.nitro.core.Promise
import com.margelo.nitro.rnvlcplyr.engine.VideoOutput

@DoNotStrip
@Keep
class HybridVlcPlayer : HybridVlcPlayerSpec() {
  private val controller = PlayerController()
  private val events = controller.events

  override var source: VlcSource?
    get() = controller.read { source }
    set(value) = controller.setSource(value)

  override var volume: Double
    get() = controller.read { settings.volumeControl.volume.toDouble() }
    set(value) = controller.setVolume(value)

  override var muted: Boolean
    get() = controller.read { settings.volumeControl.muted }
    set(value) = controller.setMuted(value)

  override var rate: Double
    get() = controller.read { settings.rate }
    set(value) = controller.setRate(value)

  override var loop: Boolean
    get() = controller.read { settings.loop }
    set(value) = controller.setLoop(value)

  override var autoPlay: Boolean
    get() = controller.read { settings.autoPlay }
    set(value) = controller.setAutoPlay(value)

  override var timeUpdateInterval: Double
    get() = controller.read { settings.timeUpdateIntervalMs.toDouble() }
    set(value) = controller.setTimeUpdateInterval(value)

  override var subtitleDelay: Double
    get() = controller.read { settings.subtitleDelayMs.toDouble() }
    set(value) = controller.setSubtitleDelay(value)

  override var audioDelay: Double
    get() = controller.read { settings.audioDelayMs.toDouble() }
    set(value) = controller.setAudioDelay(value)

  override val status: VlcStatus
    get() = controller.read { status }

  override val error: VlcError?
    get() = controller.read { media.error }

  override val currentTime: Double
    get() = controller.read { media.currentTimeMs.toDouble() }

  override val duration: Double
    get() = controller.read { media.durationMs.toDouble() }

  override val isLive: Boolean
    get() = controller.read { media.isLive }

  override val isSeekable: Boolean
    get() = controller.read { media.isSeekable }

  override val videoSize: VlcVideoSize
    get() = controller.read { media.videoSize }

  override val audioTracks: Array<VlcTrack>
    get() = controller.read { media.tracks.audioTracks }

  override val subtitleTracks: Array<VlcTrack>
    get() = controller.read { media.tracks.subtitleTracks }

  override val selectedAudioTrack: Double
    get() = controller.read { media.tracks.selectedAudioTrack }

  override val selectedSubtitleTrack: Double
    get() = controller.read { media.tracks.selectedSubtitleTrack }

  override fun play() = controller.play()

  override fun pause() = controller.pause()

  override fun stop() = controller.stop()

  override fun seek(timeMs: Double) = controller.seek(timeMs)

  override fun seekBy(deltaMs: Double) = controller.seekBy(deltaMs)

  override fun setAudioTrack(id: Double) = controller.setAudioTrack(id)

  override fun setSubtitleTrack(id: Double) = controller.setSubtitleTrack(id)

  override fun addSubtitle(uri: String, select: Boolean) = controller.addSubtitle(uri, select)

  override fun snapshot(): Promise<String> {
    val promise = Promise<String>()
    controller.captureSnapshot { result ->
      result.fold(onSuccess = promise::resolve, onFailure = promise::reject)
    }
    return promise
  }

  override fun release() = controller.release()

  override fun dispose() {
    controller.release()
    super.dispose()
  }

  override fun addOnStatusChangeListener(listener: (status: VlcStatus) -> Unit) =
    events.subscribe(events.statusChange, listener)

  override fun addOnTimeUpdateListener(listener: (event: VlcTimeUpdate) -> Unit) =
    events.subscribe(events.timeUpdate, listener)

  override fun addOnLoadedListener(listener: (event: VlcLoadedInfo) -> Unit) =
    events.subscribe(events.loaded, listener)

  override fun addOnEndedListener(listener: () -> Unit) =
    events.subscribe(events.ended) { listener() }

  override fun addOnErrorListener(listener: (error: VlcError) -> Unit) =
    events.subscribe(events.error, listener)

  override fun addOnVolumeChangeListener(listener: (event: VlcVolumeInfo) -> Unit) =
    events.subscribe(events.volumeChange, listener)

  override fun addOnTracksChangeListener(listener: (event: VlcTracksInfo) -> Unit) =
    events.subscribe(events.tracksChange, listener)

  override fun addOnVideoSizeChangeListener(listener: (size: VlcVideoSize) -> Unit) =
    events.subscribe(events.videoSizeChange, listener)

  internal fun attachOutput(output: VideoOutput) = controller.attachOutput(output)

  internal fun detachOutput(output: VideoOutput) = controller.detachOutput(output)

  internal fun reattachOutput(output: VideoOutput) = controller.reattachOutput(output)

  internal fun applyScale(output: VideoOutput) = controller.applyScale(output)
}
