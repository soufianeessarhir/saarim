package com.sarim.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sarim.app.data.FocusMode
import com.sarim.app.data.FocusScheduleManager
import timber.log.Timber

class FocusScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SCHEDULE_TRIGGER -> {
                Timber.d("Focus scheduled alarm triggered")
                FocusScheduleManager.reschedule(context)

                if (FocusMode.getStatus(context)?.phase == FocusMode.Phase.FOCUS) {
                    Timber.d("Focus already active, skipping scheduled reminder")
                    return
                }

                FocusScheduleManager.launchReminder(context, snoozeCount = 0)
            }
            ACTION_SNOOZE_TRIGGER -> {
                val snoozeCount = intent.getIntExtra(EXTRA_SNOOZE_COUNT, 1)
                Timber.d("Focus snooze triggered, count=$snoozeCount")

                if (FocusMode.getStatus(context)?.phase == FocusMode.Phase.FOCUS) {
                    Timber.d("Focus already active, cancelling snooze")
                    FocusScheduleManager.cancelSnooze(context)
                    return
                }

                FocusScheduleManager.launchReminder(context, snoozeCount = snoozeCount)
            }
        }
    }

    companion object {
        const val ACTION_SCHEDULE_TRIGGER = "com.sarim.app.focus.SCHEDULE_TRIGGER"
        const val ACTION_SNOOZE_TRIGGER = "com.sarim.app.focus.SNOOZE_TRIGGER"
        const val EXTRA_SNOOZE_COUNT = "snooze_count"
    }
}
