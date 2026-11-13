package com.margelo.nitro.rnvlcplyr.contract

import com.margelo.nitro.rnvlcplyr.VlcTrack
import com.margelo.nitro.rnvlcplyr.VlcTracksInfo
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackListTest {
  @Test
  fun dropsTheDisableEntryAndNamesUnnamedTracks() {
    val tracks = TrackList.build(listOf(EngineTrack(-1, "Disable"), EngineTrack(1, "English"), EngineTrack(2, ""), EngineTrack(5, null)))
    assertArrayEquals(arrayOf(VlcTrack(1.0, "English"), VlcTrack(2.0, "Track 2"), VlcTrack(5.0, "Track 5")), tracks)
  }

  @Test
  fun infoKeepsTheEngineSelection() {
    val info = TrackList.info(listOf(EngineTrack(-1, "Disable"), EngineTrack(3, "A")), emptyList(), 3, -1)
    assertEquals(VlcTracksInfo(arrayOf(VlcTrack(3.0, "A")), emptyArray(), 3.0, -1.0), info)
  }

  @Test
  fun selectableIdsAreKnownIdsOrDisable() {
    val tracks = arrayOf(VlcTrack(4.0, "a"))
    assertTrue(TrackList.canSelect(tracks, -1))
    assertTrue(TrackList.canSelect(tracks, 4))
    assertFalse(TrackList.canSelect(tracks, 5))
  }

  @Test
  fun aShorterListThanBeforeIsStillLoading() {
    val before = TrackList.info(listOf(EngineTrack(1, "A"), EngineTrack(2, "B")), listOf(EngineTrack(3, "C")), 1, 3)
    val audioMissing = TrackList.info(listOf(EngineTrack(1, "A")), listOf(EngineTrack(3, "C")), 1, 3)
    val subtitleMissing = TrackList.info(listOf(EngineTrack(1, "A"), EngineTrack(2, "B")), emptyList(), 1, -1)
    assertTrue(TrackList.isStillLoading(audioMissing, before))
    assertTrue(TrackList.isStillLoading(subtitleMissing, before))
    assertFalse(TrackList.isStillLoading(before, before))
  }
}
