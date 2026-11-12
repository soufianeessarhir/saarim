package com.sarim.app.security

import android.content.Context
import android.os.SystemClock
import androidx.core.content.edit

/**
 * The commitment: the period the user picks during setup, before anything else is configured,
 * during which adult-content protection cannot be turned off, weakened or removed.
 *
 * The floor is [MIN_DURATION_DAYS] days — a commitment that can be shortened to a weekend is not a
 * commitment — and the ceiling is "permanent", for the user who wants the app to be a one-way
 * decision. Starting with 0 days records a regular setup instead: the app is still protected, but
 * the recovery code can change settings normally, exactly as it did before commitments existed.
 *
 * [start] is the only way in. For a timed commitment there are three ways out: the period running
 * out, the device-owner 48-hour forced removal that takes the app off the device entirely, or the
 * release request ([requestDisable]) — the user may order an early end, confirm it with the
 * recovery code, and then has to wait [DISABLE_REQUEST_DAYS] days before confirming again. The
 * wait can be cancelled at any point, which drops the order and leaves the commitment exactly as
 * it was. A permanent commitment has no release request and no forced removal; that is still the
 * difference between the two choices.
 *
 * Time is measured the way the rest of the app measures a deadline, because this is exactly the
 * kind of thing someone will try to shorten by changing the device date:
 *  - `elapsedRealtime` never moves when the wall clock does, so it is authoritative while the
 *    device stays up;
 *  - `System.currentTimeMillis` is the only thing that survives a reboot, so it takes over then;
 *  - a persisted watermark records the furthest wall-clock instant ever observed, so moving the
 *    date *backwards* cannot inflate the remaining time either.
 *
 * Not a security boundary against a rooted device or a factory reset — no on-device app is — but
 * [enforce] re-asserts the whole protection stack on every launch, on every service start and on
 * every settings write, and [MandatoryProtection] additionally locks the system doors that lead
 * around it.
 */
object CommitmentLock {

    const val MIN_DURATION_DAYS = 90
    const val MAX_DURATION_DAYS = 3650

    /** Offered in the duration picker; "permanent" is a separate choice, not a number of days. */
    val DURATION_PRESETS_DAYS = listOf(90, 180, 365, 730)

    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** Stored duration of a commitment that never ends. */
    private const val PERMANENT = -1L

    /** Stored duration of a regular setup: protection stays on, but nothing is time-locked. */
    private const val REGULAR = -2L

    /** Stored duration of a commitment ended through the seven-day release request. */
    private const val STOPPED = -3L

    /**
     * How far the wall clock may run ahead of the stored watermark before it is rewritten. The
     * watermark only matters across a reboot, so coalescing it keeps a per-second UI tick from
     * writing a preference every second, at the cost of at most this much slack if the date is
     * rewound — against a deadline measured in months.
     */
    private const val WATERMARK_INTERVAL_MS = 60L * 60 * 1000

    /**
     * How long an order to end the commitment has to wait before it can be carried out. The delay
     * is the point: a decision made in a weak moment cannot end the protection that same night.
     */
    const val DISABLE_REQUEST_DAYS = 7

    private const val DISABLE_REQUEST_MS = DISABLE_REQUEST_DAYS * DAY_MS

    private const val PREFS_NAME = "security_prefs"
    private const val KEY_START_RTC = "commitment_start_rtc"
    private const val KEY_START_ELAPSED = "commitment_start_elapsed"
    private const val KEY_DURATION_MS = "commitment_duration_ms"
    private const val KEY_LAST_SEEN_RTC = "commitment_last_seen_rtc"

    private const val KEY_DISABLE_REQUEST_RTC = "commitment_release_request_rtc"
    private const val KEY_DISABLE_REQUEST_ELAPSED = "commitment_release_request_elapsed"
    private const val KEY_DISABLE_REQUEST_LAST_SEEN_RTC = "commitment_release_last_seen_rtc"

    /** Keys written by the previous, optional strictness lock; read so an upgrade keeps its deadline. */
    private const val LEGACY_KEY_START_RTC = "strictness_lock_start_rtc"
    private const val LEGACY_KEY_START_ELAPSED = "strictness_lock_start_elapsed"
    private const val LEGACY_KEY_DURATION_MS = "strictness_lock_duration_ms"
    private const val LEGACY_KEY_LAST_SEEN_RTC = "strictness_lock_last_seen_rtc"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** [days] limited to [MIN_DURATION_DAYS]..[MAX_DURATION_DAYS]; the floor is what makes it a commitment. */
    fun clampDurationDays(days: Int): Int = days.coerceIn(MIN_DURATION_DAYS, MAX_DURATION_DAYS)

    /**
     * Starts a commitment of [durationDays], a permanent one when it is null, or a regular
     * (unlocked) setup when it is 0. A commitment is never restarted or shortened — callers get
     * the existing deadline back instead. Returns the wall-clock instant it runs out, or 0 for a
     * permanent or regular setup.
     */
    fun start(context: Context, durationDays: Int?): Long {
        if (hasStarted(context)) return endTimeMillis(context)

        val duration = when {
            durationDays == null -> PERMANENT
            durationDays == 0 -> REGULAR
            else -> clampDurationDays(durationDays) * DAY_MS
        }
        val nowRtc = System.currentTimeMillis()
        prefs(context).edit {
            putLong(KEY_START_RTC, nowRtc)
            putLong(KEY_START_ELAPSED, SystemClock.elapsedRealtime())
            putLong(KEY_DURATION_MS, duration)
            putLong(KEY_LAST_SEEN_RTC, nowRtc)
            remove(LEGACY_KEY_START_RTC)
            remove(LEGACY_KEY_START_ELAPSED)
            remove(LEGACY_KEY_DURATION_MS)
            remove(LEGACY_KEY_LAST_SEEN_RTC)
        }
        MandatoryProtection.enforce(context)
        return endTimeMillis(context)
    }

    /** True once a commitment has ever been started, whether or not it is still running. */
    fun hasStarted(context: Context): Boolean {
        val prefs = prefs(context)
        if (prefs.contains(KEY_DURATION_MS)) return prefs.getLong(KEY_DURATION_MS, 0L) != 0L
        // Upgrading from the old optional lock: adopt its deadline rather than starting a new one.
        val legacyDuration = prefs.getLong(LEGACY_KEY_DURATION_MS, 0L)
        if (legacyDuration <= 0L) return false
        prefs.edit {
            putLong(KEY_DURATION_MS, legacyDuration)
            putLong(KEY_START_RTC, prefs.getLong(LEGACY_KEY_START_RTC, 0L))
            putLong(KEY_START_ELAPSED, prefs.getLong(LEGACY_KEY_START_ELAPSED, 0L))
            putLong(KEY_LAST_SEEN_RTC, prefs.getLong(LEGACY_KEY_LAST_SEEN_RTC, 0L))
        }
        return true
    }

    /** True when the commitment never ends. */
    fun isPermanent(context: Context): Boolean =
        hasStarted(context) && prefs(context).getLong(KEY_DURATION_MS, 0L) == PERMANENT

    /** True when setup chose the regular, unlocked path instead of a commitment. */
    fun isRegular(context: Context): Boolean =
        hasStarted(context) && prefs(context).getLong(KEY_DURATION_MS, 0L) == REGULAR

    /** True when the commitment was ended early through the release request. */
    fun isStopped(context: Context): Boolean =
        hasStarted(context) && prefs(context).getLong(KEY_DURATION_MS, 0L) == STOPPED

    fun isLocked(context: Context): Boolean = hasStarted(context) && remainingMillis(context) > 0L

    /** The configured period in days, or 0 when there is no commitment or it is permanent. */
    fun durationDays(context: Context): Int {
        val duration = prefs(context).getLong(KEY_DURATION_MS, 0L)
        return if (duration <= 0L) 0 else (duration / DAY_MS).toInt()
    }

    /** Wall-clock instant the commitment runs out, or 0 when it is permanent or not started. */
    fun endTimeMillis(context: Context): Long {
        val prefs = prefs(context)
        val duration = prefs.getLong(KEY_DURATION_MS, 0L)
        if (duration <= 0L) return 0L
        return prefs.getLong(KEY_START_RTC, 0L) + duration
    }

    /** Milliseconds left, [Long.MAX_VALUE] for a permanent commitment, or 0 when none is running. */
    fun remainingMillis(context: Context): Long {
        val prefs = prefs(context)
        val duration = prefs.getLong(KEY_DURATION_MS, 0L)
        if (duration == PERMANENT) return Long.MAX_VALUE
        if (duration <= 0L) return 0L

        val startRtc = prefs.getLong(KEY_START_RTC, 0L)
        val startElapsed = prefs.getLong(KEY_START_ELAPSED, 0L)
        val endRtc = startRtc + duration

        val nowRtc = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()

        // The furthest wall-clock instant ever observed, capped at the deadline: a clock pushed
        // wildly into the future must not be able to make the wall-clock reading negative, because
        // that would end the commitment early the next time the device rebooted onto the right time.
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
            // Never let a wall-clock jump shorten the commitment below what the monotonic clock says.
            maxOf(startElapsed + duration - nowElapsed, endRtc - trustedRtc)
        }

        return remaining.coerceAtLeast(0L)
    }

    // ------------------------------------------------------------------ release request

    /**
     * The early-end order is offered only while a timed commitment is running. A permanent
     * commitment has no way out by design, and a regular setup has nothing to end.
     */
    fun canRequestDisable(context: Context): Boolean = isLocked(context) && !isPermanent(context)

    /** True while an order to end the commitment is waiting out its seven days. */
    fun isDisableRequested(context: Context): Boolean =
        prefs(context).getLong(KEY_DISABLE_REQUEST_RTC, 0L) > 0L

    /**
     * Records the order. The commitment itself is untouched: this only starts the clock the
     * second code confirmation has to wait for. Ignored when no timed commitment is running.
     */
    fun requestDisable(context: Context) {
        if (!canRequestDisable(context) || isDisableRequested(context)) return

        val nowRtc = System.currentTimeMillis()
        prefs(context).edit {
            putLong(KEY_DISABLE_REQUEST_RTC, nowRtc)
            putLong(KEY_DISABLE_REQUEST_ELAPSED, SystemClock.elapsedRealtime())
            putLong(KEY_DISABLE_REQUEST_LAST_SEEN_RTC, nowRtc)
        }
    }

    /**
     * Drops the order and resets its timer; the commitment keeps running for its full period, as
     * if no order had ever been placed. Cancelling is always safe, so it needs no code.
     */
    fun cancelDisableRequest(context: Context) {
        prefs(context).edit {
            remove(KEY_DISABLE_REQUEST_RTC)
            remove(KEY_DISABLE_REQUEST_ELAPSED)
            remove(KEY_DISABLE_REQUEST_LAST_SEEN_RTC)
        }
    }

    /** Milliseconds left on the order's wait, or 0 when there is no order or it has matured. */
    fun disableRequestRemainingMillis(context: Context): Long {
        val prefs = prefs(context)
        val startRtc = prefs.getLong(KEY_DISABLE_REQUEST_RTC, 0L)
        if (startRtc <= 0L) return 0L

        val startElapsed = prefs.getLong(KEY_DISABLE_REQUEST_ELAPSED, 0L)
        val endRtc = startRtc + DISABLE_REQUEST_MS

        val nowRtc = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()

        val lastSeenRtc = prefs.getLong(KEY_DISABLE_REQUEST_LAST_SEEN_RTC, 0L)
        if (nowRtc - lastSeenRtc > WATERMARK_INTERVAL_MS) {
            prefs.edit { putLong(KEY_DISABLE_REQUEST_LAST_SEEN_RTC, nowRtc) }
        }
        val trustedRtc = maxOf(nowRtc, lastSeenRtc).coerceAtMost(endRtc)

        val remaining = if (nowElapsed < startElapsed) {
            // Rebooted while the order was pending: the wall clock is all there is.
            endRtc - trustedRtc
        } else {
            maxOf(startElapsed + DISABLE_REQUEST_MS - nowElapsed, endRtc - trustedRtc)
        }

        return remaining.coerceAtLeast(0L)
    }

    /** True once the waiting period is over, so the second code confirmation can end the commitment. */
    fun canCompleteDisable(context: Context): Boolean =
        isDisableRequested(context) && disableRequestRemainingMillis(context) == 0L

    /**
     * Ends a timed commitment after its release request has waited the full seven days. The
     * protection stack is re-asserted and stays on; only the settings freeze is lifted. Returns
     * false when the order is missing or still waiting.
     */
    fun stop(context: Context): Boolean {
        if (!canCompleteDisable(context)) return false

        prefs(context).edit {
            putLong(KEY_DURATION_MS, STOPPED)
            remove(KEY_DISABLE_REQUEST_RTC)
            remove(KEY_DISABLE_REQUEST_ELAPSED)
            remove(KEY_DISABLE_REQUEST_LAST_SEEN_RTC)
        }
        MandatoryProtection.enforce(context)
        return true
    }

    /**
     * Re-asserts everything the commitment pins. [MandatoryProtection] is the whole stack now
     * (visual blocking, forbidden words, browser lists, filtering DNS, Tor/torrents and the
     * system restrictions), and it is idempotent, so this is safe to call on every launch, every
     * service start and from a preference listener.
     */
    fun enforce(context: Context): Boolean = MandatoryProtection.enforce(context)
}
