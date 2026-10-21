package com.margelo.nitro.rnvlcplyr

import android.content.Context
import android.os.SystemClock
import com.margelo.nitro.rnvlcplyr.contract.AttachmentStack
import com.margelo.nitro.rnvlcplyr.contract.ChangeFilter
import com.margelo.nitro.rnvlcplyr.contract.EngineEvent
import com.margelo.nitro.rnvlcplyr.contract.MediaState
import com.margelo.nitro.rnvlcplyr.contract.PlayAction
import com.margelo.nitro.rnvlcplyr.contract.PlaybackEffect
import com.margelo.nitro.rnvlcplyr.contract.PlaybackSlave
import com.margelo.nitro.rnvlcplyr.contract.PlaybackStateMachine
import com.margelo.nitro.rnvlcplyr.contract.PlayerSettings
import com.margelo.nitro.rnvlcplyr.contract.SubtitleStyles
import com.margelo.nitro.rnvlcplyr.contract.TimeUpdateThrottle
import com.margelo.nitro.rnvlcplyr.contract.TrackList
import com.margelo.nitro.rnvlcplyr.contract.TrackSelectionRestore
import com.margelo.nitro.rnvlcplyr.contract.VlcContract
import com.margelo.nitro.rnvlcplyr.engine.EngineMediaInfo
import com.margelo.nitro.rnvlcplyr.engine.EngineSettings
import com.margelo.nitro.rnvlcplyr.engine.MainThread
import com.margelo.nitro.rnvlcplyr.engine.PlayerEngine
import com.margelo.nitro.rnvlcplyr.engine.VideoOutput
import com.margelo.nitro.rnvlcplyr.engine.VlcEngine
import com.margelo.nitro.rnvlcplyr.session.MediaSessionCoordinator
import com.margelo.nitro.rnvlcplyr.session.SessionHost
import com.margelo.nitro.rnvlcplyr.session.SessionSnapshot
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.MediaPlayer

private typealias Outbox = MutableList<() -> Unit>

class PlayerController(private val context: Context = VlcEngine.applicationContext()) : SessionHost {
  override val events = PlayerEvents(onRemoteListenerPresenceChange = { session.refresh() })

  private val lock = Any()
  private var settings = PlayerSettings()
  private val machine = PlaybackStateMachine { settings.loop }
  private val throttle = TimeUpdateThrottle()
  private val tracksFilter = ChangeFilter(TrackList.EMPTY_INFO)
  private val videoSizeFilter = ChangeFilter(MediaState.EMPTY_VIDEO_SIZE)
  private val outputs = AttachmentStack<VideoOutput>()
  private val engine = PlayerEngine(VlcEngine.libVLC(context)) { event -> onEngineEvent(event) }
  private val session = MediaSessionCoordinator(context, this)
  private var media = MediaState()
  private var source: VlcSource? = null
  private var pendingSeekMs: Long? = null
  private var pendingSessionPause = false
  private val addedSlaves = mutableListOf<PlaybackSlave>()
  private var reopenSlaves: List<PlaybackSlave> = emptyList()
  private var reopenSlavesAtMs: Long? = null
  private var trackRestore = TrackSelectionRestore.none()
  private var styledLibVLC: LibVLC? = null
  private var reloadingInPlace = false
  private var tracksBeforeReopen: VlcTracksInfo? = null
  private var released = false

  val isReleased: Boolean
    get() = locked { released }

  fun <T> read(selector: PlayerReadModel.() -> T): T = locked { PlayerReadModel(settings, machine.status, media, source).selector() }

  fun setSource(value: VlcSource?) {
    if (!locked { if (released) false else { source = value; addedSlaves.clear(); true } }) return
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
    add(session::refresh)
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

  fun setSubtitleStyle(value: VlcSubtitleStyle?) {
    val options = locked {
      if (released || !settings.setSubtitleStyle(value)) return
      SubtitleStyles.options(settings.subtitleStyle)
    }
    onMain { applySubtitleStyle(options) }
  }

  fun setAudioDelay(value: Double) = mutate {
    settings.setAudioDelay(value)
    val micros = VlcContract.delayToMicros(settings.audioDelayMs)
    add { onMain { engine.setAudioDelay(micros) } }
  }

  fun setStaysActiveInBackground(value: Boolean) = mutate {
    if (settings.staysActiveInBackground == value) return@mutate
    settings.staysActiveInBackground = value
    add { session.staysActiveChanged(value) }
  }

  fun updateSessionSettings(change: PlayerSettings.() -> Unit) = mutate {
    settings.change()
    add(session::refresh)
  }

  override fun sessionSnapshot(): SessionSnapshot =
    read { SessionSnapshot.of(this, events.remoteNext.count > 0, events.remotePrevious.count > 0) }

  override fun play() = onMain {
    val action = locked { machine.resolvePlay() }
    if (action == PlayAction.NONE) return@onMain
    session.onUserCommand()
    session.requestFocus()
    locked { pendingSessionPause = false }
    when (action) {
      PlayAction.NONE -> Unit
      PlayAction.RESUME -> engine.play()
      PlayAction.RESTART -> restartFromBeginning()
      PlayAction.RELOAD -> loadSource(locked { source }, forcePlay = true)
    }
  }

  override fun pause() = onMain {
    session.onUserCommand()
    if (locked { machine.canPause() }) engine.pause()
  }

  override fun pauseForSession() = onMain {
    val pausable = locked {
      pendingSessionPause = machine.status == VlcStatus.OPENING
      machine.canPause()
    }
    if (pausable) engine.pause()
  }

  override fun setVideoTrackEnabled(enabled: Boolean) = onMain { engine.setVideoTrackEnabled(enabled) }

  override fun stop() = onMain {
    session.onStopped()
    mutate {
      val effects = machine.stop() ?: return@mutate
      pendingSeekMs = null
      pendingSessionPause = false
      reloadingInPlace = false
      media = media.copy(currentTimeMs = 0L)
      val update = media.timeUpdate
      add { engine.stop() }
      applyEffects(effects)
      add { events.timeUpdate.emit(update) }
    }
  }

  override fun seek(timeMs: Double) {
    if (!timeMs.isNaN()) onMain { performSeek(timeMs) }
  }

  override fun seekBy(deltaMs: Double) {
    if (!deltaMs.isNaN()) onMain { performSeek(locked { media.currentTimeMs } + deltaMs) }
  }

  fun setAudioTrack(id: Double) = onMain {
    val trackId = id.toInt()
    if (locked { isSelectable(media.tracks.audioTracks, trackId).also { if (it) tracksBeforeReopen = null } }) {
      engine.selectAudioTrack(trackId)
      refreshMediaInfo()
    }
  }

  fun setSubtitleTrack(id: Double) = onMain {
    val trackId = id.toInt()
    if (locked { isSelectable(media.tracks.subtitleTracks, trackId).also { if (it) tracksBeforeReopen = null } }) {
      engine.selectSubtitleTrack(trackId)
      refreshMediaInfo()
    }
  }

  fun addAudio(uri: String, select: Boolean) =
    addSlave(PlaybackSlave.Kind.AUDIO, uri, select, "addAudio requires a source")

  fun addSubtitle(uri: String, select: Boolean) =
    addSlave(PlaybackSlave.Kind.SUBTITLE, uri, select, "addSubtitle requires a source")

  private fun addSlave(kind: PlaybackSlave.Kind, uri: String, select: Boolean, missingSourceMessage: String) {
    if (locked { released }) return
    if (locked { source == null }) throw VlcPlayerError(missingSourceMessage)
    val resolved = VlcContract.resolveMediaUri(uri) ?: return
    val slave = PlaybackSlave(kind, resolved, select)
    val needsReader = locked {
      (kind == PlaybackSlave.Kind.AUDIO && !hasAddedAudio).also { if (!it) addedSlaves.add(slave) }
    }
    onMain { if (needsReader) reloadAdding(slave) else engine.addSlave(slave) }
  }

  private val hasAddedAudio: Boolean
    get() = addedSlaves.any { it.kind == PlaybackSlave.Kind.AUDIO }

  private fun reloadAdding(slave: PlaybackSlave) = mutate {
    when {
      !machine.hasSource || machine.status == VlcStatus.ERROR -> addedSlaves.add(slave)
      machine.isPlaybackActive() -> reopenWithCurrentState(adding = slave)
      else -> reloadWithoutPlaying(slave)
    }
  }

  private fun Outbox.reloadWithoutPlaying(slave: PlaybackSlave) {
    val current = source ?: return
    val uri = VlcContract.resolveMediaUri(current.uri) ?: return
    val attached = addedSlaves.toList()
    addedSlaves.add(slave)
    reopenSlaves = reopenSlaves + slave
    val options = VlcContract.mediaOptions(current, hasAddedAudio)
    add {
      engine.load(uri, options)
      attached.forEach(engine::addSlave)
    }
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
    session.release()
    events.clear()
    MainThread.run {
      outputs.clear()
      engine.release()
      styledLibVLC?.release()
      styledLibVLC = null
    }
  }

  fun attachOutput(output: VideoOutput) {
    if (isReleased) return
    if (outputs.attach(output)) engine.show(outputs.top)
  }

  fun detachOutput(output: VideoOutput) {
    if (outputs.detach(output)) engine.show(outputs.top)
  }

  fun renewOutput(output: VideoOutput) {
    engine.renewOutput(output)
  }

  fun applyScale(output: VideoOutput) {
    engine.applyScale(output)
  }

  private fun loadSource(value: VlcSource?, forcePlay: Boolean) {
    val resolvedUri = value?.let { VlcContract.resolveMediaUri(it.uri) }
    val wantsPlay = resolvedUri != null && (forcePlay || locked { settings.autoPlay })
    if (wantsPlay) session.requestFocus()
    mutate { applySource(value, resolvedUri, wantsPlay) }
  }

  private fun Outbox.applySource(value: VlcSource?, resolvedUri: String?, playNow: Boolean) {
    media = MediaState()
    pendingSeekMs = null
    pendingSessionPause = false
    reopenSlaves = emptyList()
    reopenSlavesAtMs = null
    trackRestore = TrackSelectionRestore.none()
    reloadingInPlace = false
    tracksBeforeReopen = null
    throttle.reset()
    videoSizeFilter.reset()
    queueTracks(TrackList.EMPTY_INFO)
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
        val options = VlcContract.mediaOptions(value, hasAddedAudio = false)
        add { engine.load(resolvedUri, options) }
        applyEffects(machine.begin(resolvedUri, playNow))
        if (playNow) add { engine.play() }
      }
    }
    add { session.onSourceChanged(cleared = value == null) }
  }

  private fun restartFromBeginning() = mutate {
    media = media.copy(currentTimeMs = 0L)
    add { engine.restart() }
  }

  private fun applySubtitleStyle(options: List<String>) {
    val previous = styledLibVLC
    val next = if (options.isEmpty()) null else VlcEngine.create(context, options)
    styledLibVLC = next
    engine.useLibVLC(next ?: VlcEngine.libVLC(context))
    mutate {
      when {
        !machine.hasSource || machine.status == VlcStatus.ERROR -> Unit
        machine.isPlaybackActive() -> reopenWithCurrentState()
        else -> add { engine.stop() }
      }
    }
    previous?.release()
  }

  private fun Outbox.reopenWithCurrentState(adding: PlaybackSlave? = null) {
    val current = source ?: return
    val uri = VlcContract.resolveMediaUri(current.uri) ?: return
    if (!reloadingInPlace) {
      captureStateForReopen()
      reloadingInPlace = true
    }
    if (adding != null) {
      addedSlaves.add(adding)
      reopenSlaves = reopenSlaves + adding
      if (adding.select) trackRestore.forgetAudioSelection()
    }
    val options = VlcContract.mediaOptions(current, hasAddedAudio)
    add { engine.load(uri, options) }
    applyEffects(machine.reopen())
    add { engine.play() }
  }

  private fun captureStateForReopen() {
    if (pendingSeekMs == null && media.hasReachedPlaying && media.isSeekable && media.currentTimeMs > 0L) {
      pendingSeekMs = media.currentTimeMs
    }
    pendingSessionPause = pendingSessionPause || machine.status == VlcStatus.PAUSED
    val tracks = tracksBeforeReopen?.takeIf { TrackList.isStillLoading(media.tracks, it) } ?: media.tracks
    tracksBeforeReopen = tracks
    trackRestore = TrackSelectionRestore.capture(tracks, addedSlaves)
    reopenSlaves = trackRestore.reopenSlaves
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
    add(session::refresh)
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
      reloadingInPlace = false
      if (pendingSeekMs == null) queuePendingSessionPause()
    }
  }

  private fun onTimeChanged(timeMs: Long) {
    val seekable = engine.isSeekable
    mutate {
      if (machine.status !in TIME_TRACKING_STATUSES) return@mutate
      val target = pendingSeekMs
      if (target != null && machine.status == VlcStatus.PLAYING && seekable) {
        applyPendingSeek(target)
        return@mutate
      }
      trackTime(timeMs)
      reattachSlavesOnceSettled()
    }
  }

  private fun Outbox.applyPendingSeek(target: Long) {
    pendingSeekMs = null
    media = media.copy(currentTimeMs = target)
    add { engine.setTime(target) }
    queuePendingSessionPause()
  }

  private fun Outbox.reattachSlavesOnceSettled() {
    if (reopenSlaves.isEmpty() || pendingSeekMs != null || machine.status != VlcStatus.PLAYING) return
    val now = SystemClock.elapsedRealtime()
    val dueAt = reopenSlavesAtMs ?: (now + VlcContract.SLAVE_REATTACH_DELAY_MS).also { reopenSlavesAtMs = it }
    if (now < dueAt) return
    val slaves = reopenSlaves
    reopenSlaves = emptyList()
    reopenSlavesAtMs = null
    slaves.forEach { slave -> add { onMain { engine.addSlave(slave) } } }
  }

  private fun Outbox.queuePendingSessionPause() {
    if (pendingSessionPause) add { engine.pause() }
    pendingSessionPause = false
  }

  private fun Outbox.trackTime(timeMs: Long) {
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
    queueTrackRestore(tracks)
  }

  private fun Outbox.queueTrackRestore(tracks: VlcTracksInfo) {
    if (tracksBeforeReopen?.let { TrackList.isStillLoading(tracks, it) } == false) tracksBeforeReopen = null
    trackRestore.takeAudioTrack(tracks.audioTracks)
      ?.takeIf { it != tracks.selectedAudioTrack.toInt() }
      ?.let { id -> add { onMain { engine.selectAudioTrack(id) } } }
    trackRestore.takeSubtitleTrack(tracks.subtitleTracks)
      ?.takeIf { it != tracks.selectedSubtitleTrack.toInt() }
      ?.let { id -> add { onMain { engine.selectSubtitleTrack(id) } } }
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
        is PlaybackEffect.StatusChanged -> {
          add { events.statusChange.emit(effect.status) }
          add(session::onStatusChanged)
        }
        PlaybackEffect.Loaded -> {
          val info = media.loadedInfo
          add { events.loaded.emit(info) }
          add(session::refresh)
        }
        PlaybackEffect.Ended -> add { events.ended.emit(Unit) }
        is PlaybackEffect.Failed -> {
          media = media.copy(error = effect.error)
          add { events.error.emit(effect.error) }
        }
        PlaybackEffect.RestartForLoop -> {
          media = media.copy(currentTimeMs = 0L)
          throttle.reset()
          add { engine.restart() }
          add(session::refresh)
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
