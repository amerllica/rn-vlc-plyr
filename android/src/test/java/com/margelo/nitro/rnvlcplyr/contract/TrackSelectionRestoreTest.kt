package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcTrack
import com.margelo.nitro.rnvlcplyr.VlcTracksInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackSelectionRestoreTest {
  private fun track(id: Int, name: String) = VlcTrack(id.toDouble(), name)

  @Test
  fun mapsEmbeddedSelectionByPosition() {
    val before = VlcTracksInfo(
      arrayOf(track(1, "en"), track(2, "fr")),
      arrayOf(track(3, "en"), track(4, "fr"), track(5, "file.srt")),
      2.0,
      4.0
    )
    val restore = TrackSelectionRestore.capture(before, 1)
    assertNull(restore.selectedExternalSubtitle)
    assertNull(restore.takeSubtitleTrack(arrayOf(track(7, "en"))))
    assertEquals(8, restore.takeSubtitleTrack(arrayOf(track(7, "en"), track(8, "fr"))))
    assertNull(restore.takeSubtitleTrack(arrayOf(track(7, "en"), track(8, "fr"))))
    assertEquals(2, restore.takeAudioTrack(before.audioTracks))
  }

  @Test
  fun reportsTheSelectedExternalSubtitle() {
    val before = VlcTracksInfo(emptyArray(), arrayOf(track(3, "en"), track(4, "a.srt"), track(5, "b.srt")), -1.0, 5.0)
    val restore = TrackSelectionRestore.capture(before, 2)
    assertEquals(1, restore.selectedExternalSubtitle)
    assertNull(restore.takeSubtitleTrack(before.subtitleTracks))
  }

  @Test
  fun keepsDisabledSubtitlesDisabledAndIgnoresEmptyLists() {
    val restore = TrackSelectionRestore.capture(VlcTracksInfo(emptyArray(), arrayOf(track(3, "en"), track(4, "a.srt")), -1.0, -1.0), 1)
    assertNull(restore.selectedExternalSubtitle)
    assertEquals(VlcContract.NO_TRACK_ID, restore.takeSubtitleTrack(arrayOf(track(3, "en"))))
    assertNull(restore.takeAudioTrack(emptyArray()))
    assertNull(TrackSelectionRestore.none().takeAudioTrack(arrayOf(track(1, "en"))))
  }
}
