package com.margelo.nitro.rnvlcplyr

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.margelo.nitro.rnvlcplyr.session.ArtworkLoader
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
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
  private var server: ServerSocket? = null
  private val servedRequests = AtomicInteger()

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
    server?.close()
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

  @Test
  fun artworkOverTheSizeLimitIsRefused() {
    writeTinyPng()
    file.appendBytes(ByteArray(BYTES_OVER_THE_LIMIT))

    requestAndAwaitSettled()

    assertNull(loader.cached(uri))
  }

  @Test
  fun httpArtworkLoadsThroughARedirect() {
    val png = tinyPng()
    uri = serve { request ->
      if (request == 0) reply("302 Found", "Location: /moved.png") else reply("200 OK", "Content-Type: image/png", png)
    }

    requestAndAwaitSettled()

    assertNotNull(loader.cached(uri))
    assertEquals(listOf(0L), scheduledDelaysMs)
    assertEquals(2, servedRequests.get())
  }

  @Test
  fun httpErrorStatusFailsAfterEveryAttempt() {
    uri = serve { reply("404 Not Found") }

    requestAndAwaitSettled()

    assertNull(loader.cached(uri))
    assertEquals(ATTEMPTS, servedRequests.get())
  }

  @Test
  fun anEndlessRedirectStopsAfterFiveHops() {
    uri = serve { reply("302 Found", "Location: /again.png") }

    requestAndAwaitSettled()

    assertNull(loader.cached(uri))
    assertEquals(ATTEMPTS * REQUESTS_PER_REDIRECT_LOOP, servedRequests.get())
  }

  private fun serve(replyTo: (request: Int) -> ByteArray): String {
    val listening = ServerSocket(0, 0, InetAddress.getByName(SERVER_HOST))
    server = listening
    thread(isDaemon = true) {
      runCatching {
        while (true) {
          listening.accept().use { client ->
            val requestHead = client.getInputStream().bufferedReader()
            while (!requestHead.readLine().isNullOrEmpty()) continue
            client.getOutputStream().write(replyTo(servedRequests.getAndIncrement()))
          }
        }
      }
    }
    return "http://$SERVER_HOST:${listening.localPort}/cover.png"
  }

  private fun reply(status: String, header: String? = null, body: ByteArray = ByteArray(0)): ByteArray {
    val head = listOfNotNull("HTTP/1.1 $status", header, "Content-Length: ${body.size}", "Connection: close")
    return (head.joinToString(LINE_END) + LINE_END + LINE_END).toByteArray() + body
  }

  private fun tinyPng(): ByteArray {
    writeTinyPng()
    return file.readBytes()
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
    const val LINE_END = "\r\n"
    const val SERVER_HOST = "127.0.0.1"
    const val ATTEMPTS = 4
    const val REQUESTS_PER_REDIRECT_LOOP = 6
    const val BYTES_OVER_THE_LIMIT = 10 * 1024 * 1024 + 1
  }
}
