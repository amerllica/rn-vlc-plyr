package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcTrack
import com.margelo.nitro.rnvlcplyr.VlcTracksInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackSelectionRestoreTest {
  private fun track(id: Int, name: String) = VlcTrack(id.toDouble(), name)

  private fun audio(name: String, select: Boolean = false) = PlaybackSlave(PlaybackSlave.Kind.AUDIO, "file:///media/$name", select)

  private fun subtitle(name: String, select: Boolean = false) = PlaybackSlave(PlaybackSlave.Kind.SUBTITLE, "file:///media/$name", select)

  @Test
  fun mapsEmbeddedSelectionByPosition() {
    val before = VlcTracksInfo(
      arrayOf(track(1, "en"), track(2, "fr")),
      arrayOf(track(3, "en"), track(4, "fr"), track(5, "file.srt")),
      2.0,
      4.0
    )
    val restore = TrackSelectionRestore.capture(before, listOf(subtitle("file.srt", select = true)))
    assertEquals(listOf(subtitle("file.srt")), restore.reopenSlaves)
    assertNull(restore.takeSubtitleTrack(arrayOf(track(7, "en"))))
    assertEquals(8, restore.takeSubtitleTrack(arrayOf(track(7, "en"), track(8, "fr"))))
    assertNull(restore.takeSubtitleTrack(arrayOf(track(7, "en"), track(8, "fr"))))
    assertEquals(2, restore.takeAudioTrack(before.audioTracks))
  }

  @Test
  fun reportsTheSelectedExternalSubtitle() {
    val before = VlcTracksInfo(emptyArray(), arrayOf(track(3, "en"), track(4, "a.srt"), track(5, "b.srt")), -1.0, 5.0)
    val restore = TrackSelectionRestore.capture(before, listOf(subtitle("a.srt"), subtitle("b.srt")))
    assertEquals(listOf(subtitle("a.srt"), subtitle("b.srt", select = true)), restore.reopenSlaves)
    assertNull(restore.takeSubtitleTrack(arrayOf(track(7, "en"), track(8, "a.srt"))))
    assertEquals(9, restore.takeSubtitleTrack(arrayOf(track(7, "en"), track(8, "a.srt"), track(9, "b.srt"))))
  }

  @Test
  fun keepsDisabledSubtitlesDisabledAndIgnoresEmptyLists() {
    val restore = TrackSelectionRestore.capture(
      VlcTracksInfo(emptyArray(), arrayOf(track(3, "en"), track(4, "a.srt")), -1.0, -1.0),
      listOf(subtitle("a.srt", select = true))
    )
    assertEquals(listOf(subtitle("a.srt")), restore.reopenSlaves)
    assertEquals(VlcContract.NO_TRACK_ID, restore.takeSubtitleTrack(arrayOf(track(3, "en"))))
    assertNull(restore.takeAudioTrack(emptyArray()))
    assertNull(TrackSelectionRestore.none().takeAudioTrack(arrayOf(track(1, "en"))))
  }

  @Test
  fun reportsTheSelectedExternalAudio() {
    val before = VlcTracksInfo(arrayOf(track(1, "en"), track(2, "dub.m4a")), emptyArray(), 2.0, -1.0)
    val restore = TrackSelectionRestore.capture(before, listOf(audio("dub.m4a")))
    assertEquals(listOf(audio("dub.m4a", select = true)), restore.reopenSlaves)
    assertNull(restore.takeAudioTrack(arrayOf(track(7, "en"))))
    assertEquals(8, restore.takeAudioTrack(arrayOf(track(7, "en"), track(8, "dub.m4a"))))
  }

  @Test
  fun mapsEmbeddedAudioByPositionWhenAudioSlavesExist() {
    val before = VlcTracksInfo(arrayOf(track(1, "en"), track(2, "fr"), track(3, "dub.m4a")), emptyArray(), 2.0, -1.0)
    val restore = TrackSelectionRestore.capture(before, listOf(audio("dub.m4a", select = true)))
    assertEquals(listOf(audio("dub.m4a")), restore.reopenSlaves)
    assertNull(restore.takeAudioTrack(arrayOf(track(7, "en"))))
    assertEquals(8, restore.takeAudioTrack(arrayOf(track(7, "en"), track(8, "fr"))))
  }

  @Test
  fun reopenSlavesSelectOnlyTheSelectedExternalOfEachKind() {
    val before = VlcTracksInfo(
      arrayOf(track(1, "en"), track(2, "a.m4a"), track(3, "b.m4a")),
      arrayOf(track(4, "en"), track(5, "x.srt")),
      3.0,
      5.0
    )
    val restore = TrackSelectionRestore.capture(before, listOf(audio("a.m4a", select = true), subtitle("x.srt"), audio("b.m4a")))
    assertEquals(listOf(audio("a.m4a"), subtitle("x.srt", select = true), audio("b.m4a", select = true)), restore.reopenSlaves)
  }
}
