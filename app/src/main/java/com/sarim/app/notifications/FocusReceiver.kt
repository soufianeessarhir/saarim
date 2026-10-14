package com.sarim.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.sarim.app.data.FocusMode

class FocusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PHASE_END -> {
                // Keeps the process alive long enough for the alert sound to finish playing.
                val pending = goAsync()
                FocusMode.sync(context)
                Handler(Looper.getMainLooper()).postDelayed({ pending.finish() }, FocusNotifier.SOUND_KEEP_ALIVE_MS)
            }
            ACTION_START_FOCUS -> if (FocusMode.getStatus(context)?.phase == FocusMode.Phase.WAITING) {
                FocusMode.startFocus(context)
            }
            ACTION_SKIP_BREAK -> FocusMode.skipBreak(context)
            ACTION_RESUME -> FocusMode.resume(context)
            // Anywhere but WAITING, ending needs the recovery code.
            ACTION_STOP -> if (FocusMode.getStatus(context)?.phase == FocusMode.Phase.WAITING) {
                FocusMode.stop(context)
            }
            ACTION_REPOST -> FocusMode.sync(context)
        }
    }

    companion object {
        const val ACTION_PHASE_END = "com.sarim.app.focus.PHASE_END"
        const val ACTION_START_FOCUS = "com.sarim.app.focus.START_FOCUS"
        const val ACTION_SKIP_BREAK = "com.sarim.app.focus.SKIP_BREAK"
        const val ACTION_STOP = "com.sarim.app.focus.STOP"
        const val ACTION_RESUME = "com.sarim.app.focus.RESUME"
        const val ACTION_REPOST = "com.sarim.app.focus.REPOST"
    }
}

/** Re-arms focus mode and scheduled alarms after a reboot, an app update or a clock change. */
class FocusSystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.intent.action.TIME_SET" -> {
                FocusMode.sync(context)
                com.sarim.app.data.FocusScheduleManager.reschedule(context)
            }
        }
    }
}
