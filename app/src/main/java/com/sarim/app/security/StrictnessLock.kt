package com.sarim.app.security

import android.content.Context
import android.os.SystemClock
import androidx.core.content.edit
import com.sarim.app.data.VisualBlockingSettings

/**
 * The strictness lock: a commitment period during which the adult-content safeguards can't be
 * turned off.
 *
 * Once started, [enforce] re-asserts the pinned settings and nothing in the app releases the lock
 * early — not the recovery code, not the settings screens. There are exactly two ways out:
 * the period running out, or the device-owner 48-hour forced removal (which is the app's documented
 * last resort, and takes the app off the device entirely).
 *
 * Time is measured the way [SecurityManager.getImpulseBlockRemainingTime] measures an impulse
 * block, because a 90-day deadline is exactly the kind of thing the user will try to shorten by
 * changing the device date:
 *  - `elapsedRealtime` never moves when the wall clock does, so it is authoritative while the
 *    device stays up;
 *  - `System.currentTimeMillis` is the only thing that survives a reboot, so it takes over then;
 *  - a persisted watermark records the furthest wall-clock time ever observed, so moving the date
 *    *backwards* can't inflate the remaining time either.
 *
 * Not a security boundary against a rooted device — nothing on-device is — but it removes every
 * casual way out.
 */
object StrictnessLock {

    const val MIN_DURATION_DAYS = 90
    const val MAX_DURATION_DAYS = 3650
    const val DEFAULT_DURATION_DAYS = MIN_DURATION_DAYS

    /** Offered in the duration picker; the user can also type any value inside the allowed range. */
    val DURATION_PRESETS_DAYS = listOf(90, 180, 365, 730)

    private const val DAY_MS = 24L * 60 * 60 * 1000

    /**
     * How far the wall clock may run ahead of the stored watermark before it is rewritten. The
     * watermark only matters across a reboot, so coalescing it keeps a per-second UI tick from
     * writing a preference every second, at the cost of at most this much slack if the date is
     * rewound — against a deadline measured in months.
     */
    private const val WATERMARK_INTERVAL_MS = 60L * 60 * 1000

    private const val PREFS_NAME = "security_prefs"
    private const val KEY_START_RTC = "strictness_lock_start_rtc"
    private const val KEY_START_ELAPSED = "strictness_lock_start_elapsed"
    private const val KEY_DURATION_MS = "strictness_lock_duration_ms"
    private const val KEY_LAST_SEEN_RTC = "strictness_lock_last_seen_rtc"
    private const val KEY_BLOCK_TOR = "block_tor"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun visualBlockingPrefs(context: Context) =
        context.getSharedPreferences(VisualBlockingSettings.PREFS_NAME, Context.MODE_PRIVATE)

    /** [days] limited to [MIN_DURATION_DAYS]..[MAX_DURATION_DAYS]; the floor is what makes the lock a commitment. */
    fun clampDurationDays(days: Int): Int = days.coerceIn(MIN_DURATION_DAYS, MAX_DURATION_DAYS)

    fun isLocked(context: Context): Boolean = remainingMillis(context) > 0L

    /** True once a lock has ever been started, whether or not it is still running. */
    fun hasEverLocked(context: Context): Boolean = prefs(context).getLong(KEY_DURATION_MS, 0L) > 0L

    /** The configured period in days, or 0 when no lock was ever started. */
    fun durationDays(context: Context): Int =
        (prefs(context).getLong(KEY_DURATION_MS, 0L) / DAY_MS).toInt()

    /** Wall-clock instant the lock runs out, or 0 when no lock was ever started. */
    fun endTimeMillis(context: Context): Long {
        val prefs = prefs(context)
        val duration = prefs.getLong(KEY_DURATION_MS, 0L)
        if (duration <= 0L) return 0L
        return prefs.getLong(KEY_START_RTC, 0L) + duration
    }

    /**
     * Starts a lock of [durationDays] (clamped). A running lock is never restarted or shortened —
     * callers get the existing deadline back instead.
     *
     * Returns the wall-clock instant the lock runs out.
     */
    fun start(context: Context, durationDays: Int = DEFAULT_DURATION_DAYS): Long {
        if (isLocked(context)) return endTimeMillis(context)

        val duration = clampDurationDays(durationDays) * DAY_MS
        val nowRtc = System.currentTimeMillis()
        prefs(context).edit {
            putLong(KEY_START_RTC, nowRtc)
            putLong(KEY_START_ELAPSED, SystemClock.elapsedRealtime())
            putLong(KEY_DURATION_MS, duration)
            putLong(KEY_LAST_SEEN_RTC, nowRtc)
        }
        return nowRtc + duration
    }

    /** Milliseconds left, or 0 when there is no lock or it has run out. */
    fun remainingMillis(context: Context): Long {
        val prefs = prefs(context)
        val duration = prefs.getLong(KEY_DURATION_MS, 0L)
        if (duration <= 0L) return 0L

        val startRtc = prefs.getLong(KEY_START_RTC, 0L)
        val startElapsed = prefs.getLong(KEY_START_ELAPSED, 0L)
        val endRtc = startRtc + duration

        val nowRtc = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()

        // The furthest wall-clock instant ever observed, capped at the deadline: a clock pushed
        // wildly into the future must not be able to make the wall-clock reading negative, because
        // that would end the lock early the next time the device rebooted onto the right time.
        val lastSeenRtc = prefs.getLong(KEY_LAST_SEEN_RTC, 0L)
        if (nowRtc - lastSeenRtc > WATERMARK_INTERVAL_MS) {
            prefs.edit { putLong(KEY_LAST_SEEN_RTC, nowRtc) }
        }
        val trustedRtc = maxOf(nowRtc, lastSeenRtc).coerceAtMost(endRtc)

        val remaining = if (nowElapsed < startElapsed) {
            // elapsedRealtime restarted under us: the device rebooted, so the wall clock is all
            // there is to go on.
            endRtc - trustedRtc
        } else {
            // Never let a wall-clock jump shorten the lock below what the monotonic clock says.
            maxOf(startElapsed + duration - nowElapsed, endRtc - trustedRtc)
        }

        return remaining.coerceAtLeast(0L)
    }

    /**
     * Re-asserts what the lock pins: visual blocking on, the adult categories enforced, and the
     * Tor block on (Tor is the bypass route to adult content the user reported).
     *
     * Writes a preference only when it is out of line, so it is safe to call from a
     * SharedPreferences change listener — the write it makes can re-enter, find everything correct
     * and stop. Returns whether anything had to be fixed.
     */
    fun enforce(context: Context): Boolean {
        if (!isLocked(context)) return false

        var changed = false

        val blocking = visualBlockingPrefs(context)
        if (!blocking.getBoolean(VisualBlockingSettings.KEY_ENABLED, false)) {
            blocking.edit { putBoolean(VisualBlockingSettings.KEY_ENABLED, true) }
            changed = true
        }

        val categories = blocking.getStringSet(VisualBlockingSettings.KEY_CATEGORIES, null)
            ?: VisualBlockingSettings.DEFAULT_CATEGORIES
        val required = VisualBlockingSettings.DEFAULT_CATEGORIES
        if (!categories.containsAll(required)) {
            blocking.edit {
                putStringSet(VisualBlockingSettings.KEY_CATEGORIES, categories + required)
            }
            changed = true
        }

        val security = prefs(context)
        if (!security.getBoolean(KEY_BLOCK_TOR, false)) {
            security.edit { putBoolean(KEY_BLOCK_TOR, true) }
            changed = true
        }

        return changed
    }
}
