package com.margelo.nitro.rnvlcplyr

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.margelo.nitro.rnvlcplyr.session.ArtworkLoader
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArtworkLoaderTest {
  private val scheduledDelaysMs = CopyOnWriteArrayList<Long>()
  private lateinit var worker: ExecutorService
  private lateinit var loader: ArtworkLoader
  private lateinit var file: File
  private lateinit var uri: String

  @Before
  fun setUp() {
    worker = Executors.newSingleThreadExecutor()
    loader = ArtworkLoader { delayMs, work ->
      scheduledDelaysMs.add(delayMs)
      worker.execute(work)
    }
    file = File(TestMedia.targetContext.cacheDir, "artwork-${System.nanoTime()}.png")
    uri = Uri.fromFile(file).toString()
  }

  @After
  fun tearDown() {
    worker.shutdownNow()
    file.delete()
  }

  @Test
  fun fileArtworkLoadsAndCaches() {
    writeTinyPng()
    assertNull(requestAndAwaitSettled())
    assertNotNull(loader.cached(uri))
    assertNotNull(loader.image(TestMedia.targetContext, uri) {})
    assertEquals(listOf(0L), scheduledDelaysMs)
  }

  @Test
  fun missingArtworkRetriesThenGivesUp() {
    requestAndAwaitSettled()
    assertEquals(4, scheduledDelaysMs.size)
    assertEquals(listOf(1_000L, 3_000L, 9_000L), scheduledDelaysMs.drop(1))
    assertNull(loader.cached(uri))
    assertNull(loader.image(TestMedia.targetContext, uri) {})
    assertEquals(4, scheduledDelaysMs.size)
  }

  @Test
  fun resetAllowsANewAttempt() {
    requestAndAwaitSettled()
    writeTinyPng()
    assertNull(loader.image(TestMedia.targetContext, uri) {})
    assertEquals(4, scheduledDelaysMs.size)
    loader.reset(uri)
    requestAndAwaitSettled()
    assertNotNull(loader.cached(uri))
    assertEquals(5, scheduledDelaysMs.size)
  }

  private fun requestAndAwaitSettled(): Bitmap? {
    val settled = CountDownLatch(1)
    val image = loader.image(TestMedia.targetContext, uri) { settled.countDown() }
    assertTrue("artwork never settled", settled.await(SETTLE_TIMEOUT_S, TimeUnit.SECONDS))
    return image
  }

  private fun writeTinyPng() {
    val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
  }

  private companion object {
    const val SETTLE_TIMEOUT_S = 10L
  }
}
