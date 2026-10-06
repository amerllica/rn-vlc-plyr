package com.margelo.nitro.rnvlcplyr

import android.content.Context
import android.os.SystemClock
import com.margelo.nitro.rnvlcplyr.contract.AttachmentStack
import com.margelo.nitro.rnvlcplyr.contract.ChangeFilter
import com.margelo.nitro.rnvlcplyr.contract.EngineEvent
import com.margelo.nitro.rnvlcplyr.contract.MediaState
import com.margelo.nitro.rnvlcplyr.contract.PlayAction
import com.margelo.nitro.rnvlcplyr.contract.PlaybackEffect
import com.margelo.nitro.rnvlcplyr.contract.PlaybackStateMachine
import com.margelo.nitro.rnvlcplyr.contract.PlayerSettings
import com.margelo.nitro.rnvlcplyr.contract.TimeUpdateThrottle
import com.margelo.nitro.rnvlcplyr.contract.TrackList
import com.margelo.nitro.rnvlcplyr.contract.VlcContract
import com.margelo.nitro.rnvlcplyr.engine.EngineMediaInfo
import com.margelo.nitro.rnvlcplyr.engine.EngineSettings
import com.margelo.nitro.rnvlcplyr.engine.MainThread
import com.margelo.nitro.rnvlcplyr.engine.PlayerEngine
import com.margelo.nitro.rnvlcplyr.engine.VideoOutput
import com.margelo.nitro.rnvlcplyr.engine.VlcEngine
import org.videolan.libvlc.MediaPlayer

private typealias Outbox = MutableList<() -> Unit>

class PlayerController(private val context: Context = VlcEngine.applicationContext()) {
  val events = PlayerEvents()

  private val lock = Any()
  private var settings = PlayerSettings()
  private val machine = PlaybackStateMachine { settings.loop }
  private val throttle = TimeUpdateThrottle()
  private val tracksFilter = ChangeFilter(TrackList.EMPTY_INFO)
  private val videoSizeFilter = ChangeFilter(MediaState.EMPTY_VIDEO_SIZE)
  private val outputs = AttachmentStack<VideoOutput>()
  private val engine = PlayerEngine(VlcEngine.libVLC(context)) { event -> onEngineEvent(event) }
  private var media = MediaState()
  private var source: VlcSource? = null
  private var pendingSeekMs: Long? = null
  private var released = false

  val isReleased: Boolean
    get() = locked { released }

  fun <T> read(selector: PlayerReadModel.() -> T): T = locked { PlayerReadModel(settings, machine.status, media, source).selector() }

  fun setSource(value: VlcSource?) {
    if (!locked { if (released) false else { source = value; true } }) return
    onMain { loadSource(value, forcePlay = false) }
  }

  fun setVolume(value: Double) = mutate {
    if (settings.volumeControl.setVolume(value)) queueVolumeChange()
  }

  fun setMuted(value: Boolean) = mutate {
    if (settings.volumeControl.setMuted(value)) queueVolumeChange()
  }

  fun setRate(value: Double) = mutate {
    settings.setRate(value)
    val rate = settings.rate
    add { onMain { engine.setRate(rate) } }
  }

  fun setLoop(value: Boolean) = mutate { settings.loop = value }

  fun setAutoPlay(value: Boolean) = mutate { settings.autoPlay = value }

  fun setTimeUpdateInterval(value: Double) = mutate {
    settings.setTimeUpdateInterval(value)
    throttle.intervalMs = settings.timeUpdateIntervalMs
  }

  fun setSubtitleDelay(value: Double) = mutate {
    settings.setSubtitleDelay(value)
    val micros = VlcContract.delayToMicros(settings.subtitleDelayMs)
    add { onMain { engine.setSubtitleDelay(micros) } }
  }

  fun setAudioDelay(value: Double) = mutate {
    settings.setAudioDelay(value)
    val micros = VlcContract.delayToMicros(settings.audioDelayMs)
    add { onMain { engine.setAudioDelay(micros) } }
  }

  fun play() = onMain {
    when (locked { machine.resolvePlay() }) {
      PlayAction.NONE -> Unit
      PlayAction.RESUME -> engine.play()
      PlayAction.RESTART -> restartFromBeginning()
      PlayAction.RELOAD -> loadSource(locked { source }, forcePlay = true)
    }
  }

  fun pause() = onMain {
    if (locked { machine.canPause() }) engine.pause()
  }

  fun stop() = onMain {
    mutate {
      val effects = machine.stop() ?: return@mutate
      pendingSeekMs = null
      media = media.copy(currentTimeMs = 0L)
      val update = media.timeUpdate
      add { engine.stop() }
      applyEffects(effects)
      add { events.timeUpdate.emit(update) }
    }
  }

  fun seek(timeMs: Double) {
    if (!timeMs.isNaN()) onMain { performSeek(timeMs) }
  }

  fun seekBy(deltaMs: Double) {
    if (!deltaMs.isNaN()) onMain { performSeek(locked { media.currentTimeMs } + deltaMs) }
  }

  fun setAudioTrack(id: Double) = onMain {
    val trackId = id.toInt()
    if (locked { isSelectable(media.tracks.audioTracks, trackId) }) {
      engine.selectAudioTrack(trackId)
      refreshMediaInfo()
    }
  }

  fun setSubtitleTrack(id: Double) = onMain {
    val trackId = id.toInt()
    if (locked { isSelectable(media.tracks.subtitleTracks, trackId) }) {
      engine.selectSubtitleTrack(trackId)
      refreshMediaInfo()
    }
  }

  fun addSubtitle(uri: String, select: Boolean) {
    if (locked { released }) return
    if (locked { source == null }) throw VlcPlayerError("addSubtitle requires a source")
    val resolved = VlcContract.resolveMediaUri(uri) ?: return
    onMain { engine.addSubtitle(resolved, select) }
  }

  fun captureSnapshot(onResult: (Result<String>) -> Unit) {
    if (locked { released }) return onResult(Result.failure(VlcPlayerError(NO_FRAME_MESSAGE)))
    onMain {
      val output = outputs.top
      val size = locked { media.videoSize }
      if (output == null || !MediaState.isRenderableSize(size.width.toInt(), size.height.toInt())) {
        onResult(Result.failure(VlcPlayerError(NO_FRAME_MESSAGE)))
        return@onMain
      }
      SnapshotCapture.capture(context, output, size, onResult)
    }
  }

  fun release() {
    val wasReleased = locked {
      val previous = released
      released = true
      settings = PlayerSettings()
      machine.clear()
      media = MediaState()
      source = null
      pendingSeekMs = null
      previous
    }
    if (wasReleased) return
    events.clear()
    MainThread.run {
      outputs.clear()
      engine.release()
    }
  }

  fun attachOutput(output: VideoOutput) {
    if (isReleased) return
    if (outputs.attach(output)) engine.show(outputs.top)
  }

  fun detachOutput(output: VideoOutput) {
    if (outputs.detach(output)) engine.show(outputs.top)
  }

  fun reattachOutput(output: VideoOutput) {
    if (outputs.isTop(output)) engine.show(output)
  }

  fun applyScale(output: VideoOutput) {
    engine.applyScale(output)
  }

  private fun loadSource(value: VlcSource?, forcePlay: Boolean) = mutate {
    media = MediaState()
    pendingSeekMs = null
    throttle.reset()
    videoSizeFilter.reset()
    queueTracks(TrackList.EMPTY_INFO)
    val resolvedUri = value?.let { VlcContract.resolveMediaUri(it.uri) }
    when {
      value == null -> {
        add { engine.clear() }
        applyEffects(machine.clear())
      }
      resolvedUri == null -> {
        add { engine.clear() }
        applyEffects(machine.rejectInvalidSource())
        media = media.copy(error = VlcContract.INVALID_SOURCE_ERROR)
      }
      else -> {
        val playNow = forcePlay || settings.autoPlay
        val options = VlcContract.mediaOptions(value)
        add { engine.load(resolvedUri, options) }
        applyEffects(machine.begin(resolvedUri, playNow))
        if (playNow) add { engine.play() }
      }
    }
  }

  private fun restartFromBeginning() = mutate {
    media = media.copy(currentTimeMs = 0L)
    add { engine.restart() }
  }

  private fun performSeek(targetMs: Double) = mutate {
    if (!media.isSeekable) return@mutate
    val target = VlcContract.clampSeek(targetMs, media.durationMs)
    val requiresRestart = machine.seekRequiresRestart()
    media = media.copy(currentTimeMs = target)
    throttle.markEmitted(SystemClock.elapsedRealtime())
    val update = media.timeUpdate
    if (requiresRestart) {
      pendingSeekMs = target
      add { engine.restart() }
    } else {
      add { engine.setTime(target) }
    }
    add { events.timeUpdate.emit(update) }
  }

  private fun onEngineEvent(event: MediaPlayer.Event) {
    when (event.type) {
      MediaPlayer.Event.Opening -> onStatusEvent(EngineEvent.OPENING)
      MediaPlayer.Event.Buffering -> onStatusEvent(EngineEvent.BUFFERING)
      MediaPlayer.Event.Playing -> onPlaying()
      MediaPlayer.Event.Paused -> onStatusEvent(EngineEvent.PAUSED)
      MediaPlayer.Event.Stopped -> onStatusEvent(EngineEvent.STOPPED)
      MediaPlayer.Event.EndReached -> onStatusEvent(EngineEvent.END_REACHED)
      MediaPlayer.Event.EncounteredError -> onStatusEvent(EngineEvent.ERROR)
      MediaPlayer.Event.TimeChanged -> onTimeChanged(event.timeChanged)
      MediaPlayer.Event.LengthChanged -> onLengthChanged(event.lengthChanged)
      MediaPlayer.Event.SeekableChanged -> onSeekableChanged(event.seekable)
      MediaPlayer.Event.Vout,
      MediaPlayer.Event.ESAdded,
      MediaPlayer.Event.ESDeleted,
      MediaPlayer.Event.ESSelected -> refreshMediaInfo()
    }
  }

  private fun onStatusEvent(event: EngineEvent) {
    val engineIsPlaying = engine.isPlaying
    mutate { applyEffects(machine.onEngineEvent(event, engineIsPlaying)) }
  }

  private fun onPlaying() {
    val info = engine.readMediaInfo()
    mutate {
      if (!machine.hasSource) return@mutate
      if (info != null) applyMediaInfo(info)
      media = media.copy(hasReachedPlaying = true)
      applyEffects(machine.onEngineEvent(EngineEvent.PLAYING, true))
      val engineSettings = currentEngineSettings()
      add { engine.apply(engineSettings) }
      pendingSeekMs?.let { target -> add { engine.setTime(target) } }
      pendingSeekMs = null
    }
  }

  private fun onTimeChanged(timeMs: Long) = mutate {
    if (machine.status !in TIME_TRACKING_STATUSES) return@mutate
    media = media.copy(currentTimeMs = timeMs)
    if (machine.status == VlcStatus.PLAYING && throttle.shouldEmit(SystemClock.elapsedRealtime())) {
      val update = media.timeUpdate
      add { events.timeUpdate.emit(update) }
    }
  }

  private fun onLengthChanged(lengthMs: Long) = mutate {
    if (media.hasReachedPlaying && machine.isPlaybackActive()) media = media.copy(durationMs = VlcContract.durationFromEngine(lengthMs))
  }

  private fun onSeekableChanged(seekable: Boolean) = mutate {
    if (media.hasReachedPlaying && machine.isPlaybackActive()) media = media.copy(isSeekable = seekable)
  }

  private fun refreshMediaInfo() {
    val info = engine.readMediaInfo() ?: return
    mutate {
      if (!machine.acceptsMediaInfo() || !engine.hasMedia) return@mutate
      queueTracks(info.tracks)
      queueVideoSize(info.videoWidth, info.videoHeight)
    }
  }

  private fun Outbox.applyMediaInfo(info: EngineMediaInfo) {
    media = media.copy(durationMs = VlcContract.durationFromEngine(info.lengthMs), isSeekable = info.isSeekable)
    queueTracks(info.tracks)
    queueVideoSize(info.videoWidth, info.videoHeight)
  }

  private fun Outbox.queueTracks(tracks: VlcTracksInfo) {
    media = media.copy(tracks = tracks)
    if (tracksFilter.offer(tracks)) add { events.tracksChange.emit(tracks) }
  }

  private fun Outbox.queueVideoSize(width: Int, height: Int) {
    if (!MediaState.isRenderableSize(width, height)) return
    val size = VlcVideoSize(width.toDouble(), height.toDouble())
    media = media.copy(videoSize = size)
    if (videoSizeFilter.offer(size)) add { events.videoSizeChange.emit(size) }
  }

  private fun Outbox.queueVolumeChange() {
    val info = settings.volumeControl.info
    val engineVolume = settings.volumeControl.engineVolume
    add { onMain { engine.setVolume(engineVolume) } }
    add { events.volumeChange.emit(info) }
  }

  private fun Outbox.applyEffects(effects: List<PlaybackEffect>) {
    effects.forEach { effect ->
      when (effect) {
        is PlaybackEffect.StatusChanged -> add { events.statusChange.emit(effect.status) }
        PlaybackEffect.Loaded -> media.loadedInfo.let { info -> add { events.loaded.emit(info) } }
        PlaybackEffect.Ended -> add { events.ended.emit(Unit) }
        is PlaybackEffect.Failed -> {
          media = media.copy(error = effect.error)
          add { events.error.emit(effect.error) }
        }
        PlaybackEffect.RestartForLoop -> {
          media = media.copy(currentTimeMs = 0L)
          throttle.reset()
          add { engine.restart() }
        }
      }
    }
  }

  private fun currentEngineSettings() = EngineSettings(
    volume = settings.volumeControl.engineVolume,
    rate = settings.rate,
    subtitleDelayMicros = VlcContract.delayToMicros(settings.subtitleDelayMs),
    audioDelayMicros = VlcContract.delayToMicros(settings.audioDelayMs)
  )

  private fun isSelectable(tracks: Array<VlcTrack>, id: Int): Boolean =
    id == VlcContract.NO_TRACK_ID || TrackList.contains(tracks, id)

  private fun onMain(block: () -> Unit) {
    MainThread.run { if (!isReleased) block() }
  }

  private inline fun <T> locked(block: () -> T): T = synchronized(lock, block)

  private inline fun mutate(block: Outbox.() -> Unit) {
    val outbox: Outbox = mutableListOf()
    synchronized(lock) {
      if (released) return
      outbox.block()
    }
    outbox.forEach { it() }
  }

  private companion object {
    const val NO_FRAME_MESSAGE = SnapshotCapture.NO_FRAME_MESSAGE
    val TIME_TRACKING_STATUSES = setOf(VlcStatus.BUFFERING, VlcStatus.PLAYING, VlcStatus.PAUSED)
  }
}
