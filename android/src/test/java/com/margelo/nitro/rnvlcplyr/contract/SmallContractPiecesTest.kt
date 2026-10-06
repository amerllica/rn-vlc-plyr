package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcTrack
import com.margelo.nitro.rnvlcplyr.VlcTracksInfo
import com.margelo.nitro.rnvlcplyr.VlcVideoSize
import com.margelo.nitro.rnvlcplyr.VlcVolumeInfo
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmallContractPiecesTest {
  @Test
  fun trackListDropsDisableEntryAndNamesUnnamedTracks() {
    val tracks = TrackList.build(listOf(EngineTrack(-1, "Disable"), EngineTrack(1, "English"), EngineTrack(2, ""), EngineTrack(5, null)))
    assertArrayEquals(arrayOf(VlcTrack(1.0, "English"), VlcTrack(2.0, "Track 2"), VlcTrack(5.0, "Track 5")), tracks)
  }

  @Test
  fun trackInfoKeepsEngineSelection() {
    val info = TrackList.info(listOf(EngineTrack(-1, "Disable"), EngineTrack(3, "A")), emptyList(), 3, -1)
    assertEquals(VlcTracksInfo(arrayOf(VlcTrack(3.0, "A")), emptyArray(), 3.0, -1.0), info)
    assertTrue(TrackList.contains(info.audioTracks, 3))
    assertFalse(TrackList.contains(info.audioTracks, 4))
  }

  @Test
  fun changeFilterPassesOnlyChangedValuesAndResets() {
    val filter = ChangeFilter(TrackList.EMPTY_INFO)
    val info = TrackList.info(listOf(EngineTrack(1, "A")), emptyList(), 1, -1)
    assertFalse(filter.offer(TrackList.info(emptyList(), emptyList(), -1, -1)))
    assertTrue(filter.offer(info))
    assertFalse(filter.offer(TrackList.info(listOf(EngineTrack(1, "A")), emptyList(), 1, -1)))
    assertTrue(filter.offer(TrackList.info(listOf(EngineTrack(1, "A")), emptyList(), -1, -1)))
    filter.reset()
    assertTrue(filter.offer(info))
  }

  @Test
  fun videoSizeFilterDedupsEqualSizes() {
    val filter = ChangeFilter(MediaState.EMPTY_VIDEO_SIZE)
    assertTrue(filter.offer(VlcVideoSize(640.0, 360.0)))
    assertFalse(filter.offer(VlcVideoSize(640.0, 360.0)))
    assertTrue(filter.offer(VlcVideoSize(1280.0, 720.0)))
    assertFalse(MediaState.isRenderableSize(0, 360))
    assertTrue(MediaState.isRenderableSize(1, 1))
  }

  @Test
  fun throttleEmitsAtMostOncePerInterval() {
    val throttle = TimeUpdateThrottle(250)
    assertTrue(throttle.shouldEmit(1000))
    assertFalse(throttle.shouldEmit(1100))
    assertFalse(throttle.shouldEmit(1249))
    assertTrue(throttle.shouldEmit(1250))
    throttle.markEmitted(2000)
    assertFalse(throttle.shouldEmit(2100))
    throttle.reset()
    assertTrue(throttle.shouldEmit(2101))
    throttle.intervalMs = 1000
    assertFalse(throttle.shouldEmit(2900))
  }

  @Test
  fun muteIsEmulatedWithoutLosingVolume() {
    val volume = VolumeControl()
    assertEquals(VlcVolumeInfo(100.0, false), volume.info)
    assertTrue(volume.setVolume(30.4))
    assertFalse(volume.setVolume(30.0))
    assertTrue(volume.setMuted(true))
    assertFalse(volume.setMuted(true))
    assertEquals(0, volume.engineVolume)
    assertTrue(volume.setVolume(80.0))
    assertEquals(0, volume.engineVolume)
    assertTrue(volume.setMuted(false))
    assertEquals(80, volume.engineVolume)
    assertEquals(VlcVolumeInfo(80.0, false), volume.info)
  }

  @Test
  fun settingsClampOnWriteAndKeepDefaults() {
    val settings = PlayerSettings()
    assertEquals(1.0, settings.rate, 0.0)
    assertTrue(settings.autoPlay)
    assertFalse(settings.loop)
    assertEquals(250, settings.timeUpdateIntervalMs)
    settings.setRate(10.0)
    settings.setTimeUpdateInterval(1.0)
    settings.setSubtitleDelay(123456.0)
    settings.setAudioDelay(-0.4)
    assertEquals(4.0, settings.rate, 0.0)
    assertEquals(50, settings.timeUpdateIntervalMs)
    assertEquals(60000, settings.subtitleDelayMs)
    assertEquals(0, settings.audioDelayMs)
  }

  @Test
  fun mediaStateDefaultsAndLiveRule() {
    val initial = MediaState()
    assertNull(initial.error)
    assertEquals(MediaState.EMPTY_VIDEO_SIZE, initial.videoSize)
    assertFalse(initial.isLive)
    assertTrue(initial.copy(hasReachedPlaying = true).isLive)
    assertFalse(initial.copy(hasReachedPlaying = true, isSeekable = true).isLive)
    assertFalse(initial.copy(hasReachedPlaying = true, durationMs = 10).isLive)
  }

  @Test
  fun attachmentStackRestoresPreviousTop() {
    val stack = AttachmentStack<String>()
    assertTrue(stack.attach("a"))
    assertTrue(stack.attach("b"))
    assertFalse(stack.attach("b"))
    assertFalse(stack.detach("a"))
    assertTrue(stack.attach("a"))
    assertTrue(stack.detach("a"))
    assertEquals("b", stack.top)
    assertTrue(stack.detach("b"))
    assertNull(stack.top)
  }

  @Test
  fun listenerRemovalIsIdempotentAndFailuresAreIsolated() {
    val failures = mutableListOf<Throwable>()
    val registry = ListenerRegistry<Int>(onListenerFailure = { failures.add(it) })
    val received = mutableListOf<Int>()
    registry.add { throw IllegalStateException("boom") }
    val remove = registry.add { received.add(it) }
    registry.emit(1)
    remove()
    remove()
    registry.emit(2)
    assertEquals(listOf(1), received)
    assertEquals(2, failures.size)
    assertEquals(1, registry.count)
    registry.clear()
    assertEquals(0, registry.count)
  }
}
