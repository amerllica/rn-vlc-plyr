package com.margelo.nitro.rnvlcplyr.session

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object PlaybackServiceSetup {
  const val SERVICE_CLASS_NAME = "com.margelo.nitro.rnvlcplyr.VlcPlaybackService"
  private const val TAG = "RnVlcPlyr"
  private const val MISSING_SETUP_WARNING =
    "rn-vlc-plyr: staysActiveInBackground needs the $SERVICE_CLASS_NAME service declared in AndroidManifest.xml " +
      "with android:foregroundServiceType=\"mediaPlayback\". " +
      "See the README section \"Background audio and lock screen controls\"."

  @Volatile
  private var declared: Boolean? = null

  @Volatile
  var hasWarned: Boolean = false
    private set

  fun isDeclared(context: Context): Boolean = declared ?: queryDeclared(context).also { declared = it }

  fun warnIfMissing(context: Context) {
    if (isDeclared(context)) return
    val isFirstWarning = synchronized(this) {
      val first = !hasWarned
      hasWarned = true
      first
    }
    if (isFirstWarning) Log.w(TAG, MISSING_SETUP_WARNING)
  }

  private fun queryDeclared(context: Context): Boolean = try {
    context.packageManager.getServiceInfo(ComponentName(context.packageName, SERVICE_CLASS_NAME), 0)
    true
  } catch (missing: PackageManager.NameNotFoundException) {
    false
  }
}
