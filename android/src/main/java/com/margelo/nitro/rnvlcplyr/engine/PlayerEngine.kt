package com.margelo.nitro.rnvlcplyr.engine

import android.net.Uri
import com.margelo.nitro.rnvlcplyr.VlcTracksInfo
import com.margelo.nitro.rnvlcplyr.contract.EngineTrack
import com.margelo.nitro.rnvlcplyr.contract.TrackList
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia

data class EngineSettings(
  val volume: Int,
  val rate: Double,
  val subtitleDelayMicros: Long,
  val audioDelayMicros: Long
)

data class EngineMediaInfo(
  val lengthMs: Long,
  val isSeekable: Boolean,
  val tracks: VlcTracksInfo,
  val videoWidth: Int,
  val videoHeight: Int
)

private data class MediaRequest(val uri: String, val options: List<String>)

class PlayerEngine(
  private val libVLC: LibVLC,
  private val onEvent: (MediaPlayer.Event) -> Unit
) {
  private var player: MediaPlayer? = null
  private var shownOutput: VideoOutput? = null
  private var mediaRequest: MediaRequest? = null

  init {
    player = createPlayer()
  }

  val isPlaying: Boolean
    get() = player?.isPlaying ?: false

  val hasMedia: Boolean
    get() = player?.hasMedia() ?: false

  fun load(uri: String, options: List<String>) {
    mediaRequest = MediaRequest(uri, options)
    replacePlayerWithFreshMedia()
  }

  fun clear() {
    mediaRequest = null
    replacePlayerWithFreshMedia()
  }

  fun play() {
    player?.play()
  }

  fun pause() {
    player?.pause()
  }

  fun stop() {
    replacePlayerWithFreshMedia()
  }

  fun restart() {
    replacePlayerWithFreshMedia()
    player?.play()
  }

  fun setTime(timeMs: Long) {
    player?.setTime(timeMs)
  }

  fun setVolume(volume: Int) {
    player?.setVolume(volume)
  }

  fun setRate(rate: Double) {
    player?.setRate(rate.toFloat())
  }

  fun setSubtitleDelay(delayMicros: Long) {
    player?.setSpuDelay(delayMicros)
  }

  fun setAudioDelay(delayMicros: Long) {
    player?.setAudioDelay(delayMicros)
  }

  fun apply(settings: EngineSettings) {
    setVolume(settings.volume)
    setRate(settings.rate)
    setSubtitleDelay(settings.subtitleDelayMicros)
    setAudioDelay(settings.audioDelayMicros)
  }

  fun selectAudioTrack(id: Int) {
    player?.setAudioTrack(id)
  }

  fun selectSubtitleTrack(id: Int) {
    player?.setSpuTrack(id)
  }

  fun addSubtitle(uri: String, select: Boolean) {
    player?.addSlave(IMedia.Slave.Type.Subtitle, Uri.parse(uri), select)
  }

  fun readMediaInfo(): EngineMediaInfo? {
    val activePlayer = player ?: return null
    val videoTrack = activePlayer.currentVideoTrack
    return EngineMediaInfo(
      lengthMs = activePlayer.length,
      isSeekable = activePlayer.isSeekable,
      tracks = readTracks(activePlayer),
      videoWidth = videoTrack?.width ?: 0,
      videoHeight = videoTrack?.height ?: 0
    )
  }

  fun show(output: VideoOutput?) {
    val activePlayer = player ?: return
    if (shownOutput != null) activePlayer.detachViews()
    shownOutput = output
    attachShownOutput(activePlayer)
  }

  fun applyScale(output: VideoOutput) {
    if (shownOutput === output) player?.videoScale = output.scaleType
  }

  fun release() {
    val activePlayer = player ?: return
    player = null
    retire(activePlayer)
    shownOutput = null
    mediaRequest = null
  }

  private fun createPlayer(): MediaPlayer {
    val created = MediaPlayer(libVLC)
    created.setEventListener { event -> if (player === created) onEvent(event) }
    return created
  }

  private fun replacePlayerWithFreshMedia() {
    val previous = player ?: return
    retire(previous)
    val fresh = createPlayer()
    player = fresh
    attachShownOutput(fresh)
    assignMedia(fresh)
  }

  private fun retire(retiring: MediaPlayer) {
    retiring.setEventListener(null)
    if (shownOutput != null) retiring.detachViews()
    PlayerRetirement.retire(retiring)
  }

  private fun attachShownOutput(target: MediaPlayer) {
    val output = shownOutput ?: return
    target.attachViews(output.videoLayout, null, ENABLE_SUBTITLES, output.usesTextureView)
    target.videoScale = output.scaleType
  }

  private fun assignMedia(target: MediaPlayer) {
    val request = mediaRequest ?: return
    val media = Media(libVLC, Uri.parse(request.uri))
    request.options.forEach(media::addOption)
    target.media = media
    media.release()
  }

  private fun readTracks(activePlayer: MediaPlayer): VlcTracksInfo =
    TrackList.info(
      audio = activePlayer.audioTracks.toEngineTracks(),
      subtitles = activePlayer.spuTracks.toEngineTracks(),
      selectedAudio = activePlayer.audioTrack,
      selectedSubtitle = activePlayer.spuTrack
    )

  private fun Array<MediaPlayer.TrackDescription>?.toEngineTracks(): List<EngineTrack> =
    this?.map { EngineTrack(it.id, it.name) }.orEmpty()

  private companion object {
    const val ENABLE_SUBTITLES = true
  }
}
