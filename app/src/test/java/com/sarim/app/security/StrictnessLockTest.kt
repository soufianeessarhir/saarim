package com.sarim.app.security

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import com.sarim.app.data.VisualBlockingSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for the strictness lock: the commitment-period rules, and the clock-tampering
 * resistance of its deadline.
 *
 * The remaining time is recomputed from preferences rather than stored, so these tests rewind the
 * recorded start the same way a user moving the device date would, exactly like
 * [SecurityManagerTest] does for the impulse block.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StrictnessLockTest {

    private val dayMs = 24L * 60 * 60 * 1000

    private lateinit var context: Context

    private val securityPrefs get() =
        context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)

    private val blockingPrefs get() =
        context.getSharedPreferences(VisualBlockingSettings.PREFS_NAME, Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        securityPrefs.edit { clear() }
        blockingPrefs.edit { clear() }
    }

    // ------------------------------------------------------------------ duration rules

    @Test
    fun duration_is_clamped_to_the_ninety_day_floor() {
        assertEquals(StrictnessLock.MIN_DURATION_DAYS, StrictnessLock.clampDurationDays(1))
        assertEquals(StrictnessLock.MIN_DURATION_DAYS, StrictnessLock.clampDurationDays(0))
        assertEquals(StrictnessLock.MIN_DURATION_DAYS, StrictnessLock.clampDurationDays(-30))
        assertEquals(StrictnessLock.MIN_DURATION_DAYS, StrictnessLock.clampDurationDays(89))
        assertEquals(90, StrictnessLock.clampDurationDays(90))
        assertEquals(180, StrictnessLock.clampDurationDays(180))
    }

    @Test
    fun duration_is_clamped_to_the_maximum() {
        assertEquals(StrictnessLock.MAX_DURATION_DAYS, StrictnessLock.clampDurationDays(999_999))
    }

    @Test
    fun start_below_the_floor_still_locks_for_ninety_days() {
        StrictnessLock.start(context, 1)
        assertEquals(90, StrictnessLock.durationDays(context))
        assertTrue(StrictnessLock.isLocked(context))

        val remaining = StrictnessLock.remainingMillis(context)
        assertTrue("remaining should be close to 90 days", remaining in 89 * dayMs..90 * dayMs)
    }

    // ------------------------------------------------------------------ lifecycle

    @Test
    fun no_lock_until_started() {
        assertFalse(StrictnessLock.isLocked(context))
        assertFalse(StrictnessLock.hasEverLocked(context))
        assertEquals(0L, StrictnessLock.remainingMillis(context))
        assertEquals(0L, StrictnessLock.endTimeMillis(context))
        assertEquals(0, StrictnessLock.durationDays(context))
    }

    @Test
    fun start_records_the_chosen_duration() {
        val end = StrictnessLock.start(context, 365)

        assertTrue(StrictnessLock.isLocked(context))
        assertTrue(StrictnessLock.hasEverLocked(context))
        assertEquals(365, StrictnessLock.durationDays(context))
        assertTrue(end > System.currentTimeMillis())
        assertEquals(end, StrictnessLock.endTimeMillis(context))
    }

    @Test
    fun a_running_lock_is_never_restarted_or_shortened() {
        StrictnessLock.start(context, 90)
        val firstEnd = StrictnessLock.endTimeMillis(context)

        // A second start must not be a way to pick a different (or shorter) period.
        StrictnessLock.start(context, 365)

        assertEquals(90, StrictnessLock.durationDays(context))
        assertEquals(firstEnd, StrictnessLock.endTimeMillis(context))
    }

    @Test
    fun elapsed_period_releases_the_lock() {
        StrictnessLock.start(context, 90)

        // Rewind both clocks so the period has just run out.
        val expired = 91 * dayMs
        securityPrefs.edit {
            putLong("strictness_lock_start_rtc", System.currentTimeMillis() - expired)
            putLong("strictness_lock_start_elapsed", System.currentTimeMillis() - expired)
        }

        assertFalse(StrictnessLock.isLocked(context))
        assertEquals(0L, StrictnessLock.remainingMillis(context))
        // Still recorded as a past lock, so the UI can say "your period ended" rather than "never locked".
        assertTrue(StrictnessLock.hasEverLocked(context))
    }

    // ------------------------------------------------------------------ clock tampering

    @Test
    fun moving_the_date_forward_does_not_shorten_the_lock() {
        StrictnessLock.start(context, 90)

        // Wall clock claims the period started 91 days ago; elapsedRealtime says otherwise.
        securityPrefs.edit {
            putLong("strictness_lock_start_rtc", System.currentTimeMillis() - 91 * dayMs)
        }

        val remaining = StrictnessLock.remainingMillis(context)
        assertTrue("the monotonic clock must keep the lock alive", remaining > 89 * dayMs)
    }

    @Test
    fun a_reboot_falls_back_to_the_wall_clock() {
        StrictnessLock.start(context, 90)

        // elapsedRealtime going backwards is what the device reboot looks like.
        securityPrefs.edit {
            putLong("strictness_lock_start_elapsed", System.currentTimeMillis() + 60_000L)
        }

        val remaining = StrictnessLock.remainingMillis(context)
        assertTrue("the lock must survive a reboot", remaining > 89 * dayMs)
    }

    @Test
    fun a_reboot_cannot_be_used_to_reset_the_date_backwards() {
        StrictnessLock.start(context, 90)

        // The device saw 30 days of the lock pass, then rebooted with the date set back to the
        // start: the watermark is the only record of how much time was really served.
        val startRtc = securityPrefs.getLong("strictness_lock_start_rtc", 0L)
        securityPrefs.edit {
            putLong("strictness_lock_last_seen_rtc", startRtc + 30 * dayMs)
            putLong("strictness_lock_start_elapsed", System.currentTimeMillis() + 60_000L)
        }

        val remaining = StrictnessLock.remainingMillis(context)
        assertTrue(
            "only the ~60 days left should remain, got $remaining",
            remaining in 59 * dayMs..61 * dayMs
        )
    }

    @Test
    fun an_absurd_forward_jump_cannot_make_the_remaining_time_negative() {
        StrictnessLock.start(context, 90)

        securityPrefs.edit {
            putLong("strictness_lock_last_seen_rtc", System.currentTimeMillis() + 3650 * dayMs)
            putLong("strictness_lock_start_elapsed", System.currentTimeMillis() + 60_000L)
        }

        assertEquals(0L, StrictnessLock.remainingMillis(context))
    }

    // ------------------------------------------------------------------ enforcement

    @Test
    fun enforce_does_nothing_without_a_lock() {
        blockingPrefs.edit {
            putBoolean(VisualBlockingSettings.KEY_ENABLED, false)
            putStringSet(VisualBlockingSettings.KEY_CATEGORIES, emptySet())
        }
        securityPrefs.edit { putBoolean("block_tor", false) }

        assertFalse(StrictnessLock.enforce(context))

        assertFalse(blockingPrefs.getBoolean(VisualBlockingSettings.KEY_ENABLED, true))
        assertFalse(securityPrefs.getBoolean("block_tor", true))
    }

    @Test
    fun enforce_re_asserts_the_pinned_settings_while_locked() {
        StrictnessLock.start(context, 90)
        blockingPrefs.edit {
            putBoolean(VisualBlockingSettings.KEY_ENABLED, false)
            putStringSet(VisualBlockingSettings.KEY_CATEGORIES, emptySet())
        }
        securityPrefs.edit { putBoolean("block_tor", false) }

        assertTrue(StrictnessLock.enforce(context))

        assertTrue(blockingPrefs.getBoolean(VisualBlockingSettings.KEY_ENABLED, false))
        assertTrue(
            blockingPrefs.getStringSet(VisualBlockingSettings.KEY_CATEGORIES, emptySet())!!
                .containsAll(VisualBlockingSettings.DEFAULT_CATEGORIES)
        )
        assertTrue(securityPrefs.getBoolean("block_tor", false))
    }

    @Test
    fun enforce_keeps_categories_the_user_added() {
        StrictnessLock.start(context, 90)
        blockingPrefs.edit {
            putStringSet(VisualBlockingSettings.KEY_CATEGORIES, setOf("drawings"))
        }

        StrictnessLock.enforce(context)

        val categories = blockingPrefs.getStringSet(VisualBlockingSettings.KEY_CATEGORIES, emptySet())!!
        assertTrue(categories.contains("drawings"))
        assertTrue(categories.containsAll(VisualBlockingSettings.DEFAULT_CATEGORIES))
    }

    @Test
    fun enforce_is_idempotent_so_it_cannot_loop_from_a_prefs_listener() {
        StrictnessLock.start(context, 90)

        assertTrue("first pass has fixing to do", StrictnessLock.enforce(context))
        assertFalse("second pass must be a no-op", StrictnessLock.enforce(context))
    }
}
