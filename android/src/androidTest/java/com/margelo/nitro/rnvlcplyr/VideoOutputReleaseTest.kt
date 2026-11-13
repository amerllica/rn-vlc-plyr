package com.margelo.nitro.rnvlcplyr

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoOutputReleaseTest {
  @Test
  fun releaseTellsEveryAttachedOutput() {
    TestMedia.warmLibVLC()
    val controller = PlayerController(TestMedia.targetContext)
    ActivityScenario.launch(VideoHostActivity::class.java).use { scenario ->
      val outputs = mutableListOf<HostVideoOutput>()
      scenario.onActivity { activity ->
        repeat(ATTACHED_OUTPUTS) { outputs.add(activity.createOutput(usesTextureView = true).also(controller::attachOutput)) }
      }

      controller.release()
      InstrumentationRegistry.getInstrumentation().waitForIdleSync()

      assertEquals(List(ATTACHED_OUTPUTS) { true }, outputs.map { it.playerReleased })
    }
  }

  private companion object {
    const val ATTACHED_OUTPUTS = 2
  }
}
