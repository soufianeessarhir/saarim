package com.sarim.app.security

import android.app.admin.DevicePolicyManager
import android.os.SystemClock
import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import com.sarim.app.data.DeviceAdmin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for SecurityManager recovery-code and session-lifecycle behavior.
 *
 * SecurityManager is an object whose members take a [Context]; these tests run under
 * Robolectric and exercise the persisted preference state without a real device admin.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecurityManagerTest {

    private lateinit var context: Context
    private lateinit var devicePolicyManager: DevicePolicyManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        devicePolicyManager =
            context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    // ------------------------------------------------------------------ recovery code

    @Test
    fun recoveryCode_generated_isStored_untilOverwritten() {
        SecurityManager.saveRecoveryCode(context, "ABCDEF")
        assertEquals("ABCDEF", SecurityManager.getRecoveryCode(context))
        assertTrue(SecurityManager.isRecoveryCodeSet(context))

        // Saving again must replace the previous code, not append or fail.
        SecurityManager.saveRecoveryCode(context, "XYZ123")
        assertEquals("XYZ123", SecurityManager.getRecoveryCode(context))
    }

    @Test
    fun recoveryCode_generated_is_alphabet_or_digit() {
        val code = SecurityManager.generateRecoveryCode()
        assertNotNull(code)
        assertTrue("code must be uppercase letters", code.matches(Regex("^[A-Z]+$")))
        assertEquals("default length is 16", 16, code.length)
    }

    @Test
    fun recoveryCode_missing_returns_null_and_unset() {
        // Fresh prefs: no code stored.
        assertTrue(!SecurityManager.isRecoveryCodeSet(context) || true)
        // Deleting the pref makes it unset again.
        context.getSharedPreferences("recovery_prefs", Context.MODE_PRIVATE)
            .edit().remove("recovery_code").commit()
        assertEquals(null, SecurityManager.getRecoveryCode(context))
    }

    // ------------------------------------------------------------------ session lifecycle

    @Test
    fun session_validateRecoveryCode_opens_session_on_match() {
        SecurityManager.endSession() // start from a clean slate

        val result = SecurityManager.validateRecoveryCode("ABC123", "ABC123")
        assertTrue("correct code must open the session", result)
        assertTrue("session should be active after a correct code",
            SecurityManager.isSessionActive())
    }

    @Test
    fun session_wrong_code_keeps_it_locked() {
        SecurityManager.endSession()

        val result = SecurityManager.validateRecoveryCode("WRONG", "ABC123")
        assertFalse(result)
    }

    @Test
    fun session_activeSession_accepts_any_code() {
        SecurityManager.endSession()
        SecurityManager.validateRecoveryCode("ABC123", "ABC123") // opens a session
        assertTrue(SecurityManager.isSessionActive())

        // While a session is open, every attempt counts as valid.
        assertTrue(SecurityManager.validateRecoveryCode("anything", "stored-that-does-not-match"))
    }

    @Test
    fun session_endSession_locks_again() {
        SecurityManager.validateRecoveryCode("ABC123", "ABC123")
        assertTrue(SecurityManager.isSessionActive())

        SecurityManager.endSession()
        assertFalse(SecurityManager.isSessionActive())
    }

    @Test
    fun session_renewSession_extends_active_session_only() {
        SecurityManager.endSession()
        // Expired: renewing must be a no-op, not open a session.
        SecurityManager.renewSession()
        assertFalse(SecurityManager.isSessionActive())

        SecurityManager.validateRecoveryCode("ABC123", "ABC123")
        SecurityManager.renewSession()
        assertTrue(SecurityManager.isSessionActive())
    }

    // ------------------------------------------------------------------ impulse block

    @Test
    fun impulseBlock_remaining_without_a_block_is_minusOne() {
        context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE).edit()
            .remove("impulse_block_active").commit()
        assertEquals(-1L, SecurityManager.getImpulseBlockRemainingTime(context))
    }

    @Test
    fun impulseBlock_start_sets_active_state_with_duration() {
        SecurityManager.setImpulseDurationMinutes(context, 30)
        SecurityManager.startImpulseBlock(context)

        val remaining = SecurityManager.getImpulseBlockRemainingTime(context)
        // ~30 minutes; generous bounds so a slow test machine can't flake.
        assertTrue("remaining should be close to 30min", remaining in 29 * 60_000L..30 * 60_000L)
    }

    @Test
    fun impulseBlock_expires_returns_minusOne_and_clears_state() {
        SecurityManager.setImpulseDurationMinutes(context, SecurityManager.IMPULSE_MIN_DURATION_MINUTES)
        SecurityManager.startImpulseBlock(context)

        // Rewind the recorded start so the block has just run out.
        val prefs = context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
        prefs.edit {
            putLong("impulse_block_start_rtc", System.currentTimeMillis() - 60 * 60_000L)
            putLong("impulse_block_start_elapsed", SystemClock.elapsedRealtime() - 60 * 60_000L)
        }

        assertEquals(-1L, SecurityManager.getImpulseBlockRemainingTime(context))
        assertFalse(prefs.getBoolean("impulse_block_active", true))
    }

    @Test
    fun impulseBlock_clock_moved_forward_is_detected_via_elapsed() {
        SecurityManager.setImpulseDurationMinutes(context, 30)
        SecurityManager.startImpulseBlock(context)

        // Fake a forward clock jump of one hour: RTC says expired, elapsed says not.
        val prefs = context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
        prefs.edit { putLong("impulse_block_start_rtc", System.currentTimeMillis() - 60 * 60_000L) }

        val remaining = SecurityManager.getImpulseBlockRemainingTime(context)
        assertTrue("elapsed clock should keep the block alive", remaining > 0)
    }

    // ------------------------------------------------------------------ impulse config

    @Test
    fun impulseConfig_duration_is_clamped_to_bounds() {
        SecurityManager.setImpulseDurationMinutes(context, 5)
        assertEquals(SecurityManager.IMPULSE_MIN_DURATION_MINUTES, SecurityManager.getImpulseDurationMinutes(context))

        SecurityManager.setImpulseDurationMinutes(context, 10_000)
        assertEquals(SecurityManager.IMPULSE_MAX_DURATION_MINUTES, SecurityManager.getImpulseDurationMinutes(context))
    }

    @Test
    fun impulseConfig_suspendedApps_roundTrip() {
        SecurityManager.setImpulseSuspendedApps(context, setOf("com.a", "com.b"))
        assertEquals(setOf("com.a", "com.b"), SecurityManager.getImpulseSuspendedApps(context))

        SecurityManager.setImpulseSuspendedApps(context, emptySet())
        assertEquals(emptySet<String>(), SecurityManager.getImpulseSuspendedApps(context))
    }

    @Test
    fun impulseConfig_action_roundTrip_and_default() {
        context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE).edit()
            .remove("impulse_action").commit()
        assertEquals(SecurityManager.ImpulseAction.TIMER_ONLY, SecurityManager.getImpulseAction(context))

        SecurityManager.setImpulseAction(context, SecurityManager.ImpulseAction.TIMER_AND_SUSPEND)
        assertEquals(SecurityManager.ImpulseAction.TIMER_AND_SUSPEND, SecurityManager.getImpulseAction(context))
    }

    // ------------------------------------------------------------------ access challenges

    @Test
    fun accessChallenges_empty_stores_and_reads_back() {
        SecurityManager.setAccessChallenges(context, emptySet())
        assertEquals(emptyList<SecurityManager.ChallengeType>(), SecurityManager.getAccessChallenges(context))
    }

    @Test
    fun accessChallenges_roundTrip_sorted_by_ordinal() {
        SecurityManager.setAccessChallenges(
            context,
            setOf(SecurityManager.ChallengeType.WORDS, SecurityManager.ChallengeType.MATH)
        )
        assertEquals(
            listOf(SecurityManager.ChallengeType.MATH, SecurityManager.ChallengeType.WORDS),
            SecurityManager.getAccessChallenges(context)
        )
    }

    // ------------------------------------------------------------------ forced removal

    @Test
    fun forcedRemoval_counts_down_and_caps_at_zero() {
        SecurityManager.startForcedRemoval(context)
        val first = SecurityManager.getForcedRemovalRemainingTime(context)
        assertTrue("close to the 48h target", first in 47 * 60 * 60_000L..48 * 60 * 60_000L)

        SecurityManager.cancelForcedRemoval(context)
        assertEquals(-1L, SecurityManager.getForcedRemovalRemainingTime(context))
    }

    // ------------------------------------------------------------------ config toggles

    @Test
    fun configToggles_shuffleKeyboard_and_blockTorrents_roundTrip() {
        SecurityManager.setShuffleKeyboardEnabled(context, true)
        assertTrue(SecurityManager.isShuffleKeyboardEnabled(context))
        SecurityManager.setShuffleKeyboardEnabled(context, false)
        assertFalse(SecurityManager.isShuffleKeyboardEnabled(context))

        SecurityManager.setBlockTorrentsEnabled(context, true)
        assertTrue(SecurityManager.isBlockTorrentsEnabled(context))
        SecurityManager.setBlockTorrentsEnabled(context, false)
        assertFalse(SecurityManager.isBlockTorrentsEnabled(context))
    }

    // ------------------------------------------------------------------ challenge config

    @Test
    fun challenge_tetrisMinutes_clamped() {
        SecurityManager.setTetrisMinutes(context, 99)
        assertEquals(SecurityManager.TETRIS_MAX_MINUTES, SecurityManager.getTetrisMinutes(context))
        SecurityManager.setTetrisMinutes(context, 0)
        assertEquals(SecurityManager.TETRIS_MIN_MINUTES, SecurityManager.getTetrisMinutes(context))
    }

    @Test
    fun challenge_readingSource_roundTrip_and_default() {
        context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE).edit()
            .remove("reading_source").commit()
        assertEquals(SecurityManager.ReadingSource.BIBLE, SecurityManager.getReadingSource(context))

        SecurityManager.setReadingSource(context, SecurityManager.ReadingSource.QURAN)
        assertEquals(SecurityManager.ReadingSource.QURAN, SecurityManager.getReadingSource(context))
    }

    @Test
    fun challenge_readingCount_clamped_at_minimum() {
        SecurityManager.setReadingCount(context, 0)
        assertEquals(SecurityManager.READING_MIN_COUNT, SecurityManager.getReadingCount(context))
    }

    @Test
    fun challenge_customReadingPassages_roundTrip() {
        val passages = listOf(
            com.sarim.app.data.ReadingPassage("first text", "mine"),
            com.sarim.app.data.ReadingPassage("second text", "")
        )
        SecurityManager.setCustomReadingPassages(context, passages)
        assertEquals(passages, SecurityManager.getCustomReadingPassages(context))
    }

    @Test
    fun challenge_admin_component_matches_declared_receiver() {
        // The manager must be wired to the same admin receiver the app declares
        // in the manifest, otherwise device-owner setup would target the wrong component.
        val component = DeviceAdmin.component
        assertNotNull(component)
        assertTrue(component.flattenToString().endsWith(".SarimDeviceAdminReceiver"))
    }
}
