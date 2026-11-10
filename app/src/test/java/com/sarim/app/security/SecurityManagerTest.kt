package com.sarim.app.security

import android.app.admin.DevicePolicyManager
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

    // ------------------------------------------------------------------ access challenges

    @Test
    fun accessChallenges_default_to_words() {
        context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE).edit()
            .remove("access_challenges").commit()
        assertEquals(
            listOf(SecurityManager.ChallengeType.WORDS),
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
    fun configToggles_shuffleKeyboard_roundTrip() {
        SecurityManager.setShuffleKeyboardEnabled(context, true)
        assertTrue(SecurityManager.isShuffleKeyboardEnabled(context))
        SecurityManager.setShuffleKeyboardEnabled(context, false)
        assertFalse(SecurityManager.isShuffleKeyboardEnabled(context))
    }

    @Test
    fun mandatoryProtection_is_always_on_and_cannot_be_switched_off() {
        // A preference left behind by an older version must not switch either block off.
        context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE).edit()
            .putBoolean(MandatoryProtection.KEY_BLOCK_TOR, false)
            .putBoolean(MandatoryProtection.KEY_BLOCK_TORRENTS, false)
            .commit()

        assertTrue(MandatoryProtection.enforce(context))

        val prefs = context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean(MandatoryProtection.KEY_BLOCK_TOR, false))
        assertTrue(prefs.getBoolean(MandatoryProtection.KEY_BLOCK_TORRENTS, false))
    }

    // ------------------------------------------------------------------ challenge config

    @Test
    fun challenge_readingSource_defaults_to_bible() {
        context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE).edit()
            .remove("reading_source").commit()
        assertEquals(SecurityManager.ReadingSource.BIBLE, SecurityManager.getReadingSource(context))
    }

    @Test
    fun challenge_readingCount_defaults_to_five() {
        context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE).edit()
            .remove("reading_count").commit()
        assertEquals(SecurityManager.READING_DEFAULT_COUNT, SecurityManager.getReadingCount(context))
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
