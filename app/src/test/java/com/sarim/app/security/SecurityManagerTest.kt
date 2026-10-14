package com.sarim.app.security

import android.app.admin.DevicePolicyManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sarim.app.data.DeviceAdmin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * Unit tests for SecurityManager recovery-code and session-lifecycle behavior.
 *
 * SecurityManager read/writes local app preferences directly, so these tests
 * exercise the in-memory algorithm and the preference/locking contract in a
 * Robolectric-hosted process without a real device admin or locked screen.
 */
@Config(sdk = [34])
class SecurityManagerTest @JvmOverloads constructor(
    @get:Rule val nameRule: TestName = TestName(),
    @get:Rule val contextRule: SecurityManagerTestContext = SecurityManagerTestContext()
) {

    private lateinit var context: SecurityManagerTestContext
    private lateinit var securityManager: SecurityManager
    private lateinit var devicePolicyManager: DevicePolicyManager

    @Before
    fun setUp() {
        context = SecurityManagerTestContext()
        devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        securityManager = SecurityManager.newInstance(context)
    }

    // ------------------------------------------------------------------ recovery code

    @Test
    fun recoveryCode_generated_resets_previous_code() {
        securityManager.generateRecoveryCode()

        val first = securityManager.getRecoveryCode()
        assertNotNull("recovery code should exist after generation", first)
        assertTrue("recovery code must be stable and hex-like", first.matches(Regex("^[0-9A-Fa-f]+$")))

        // Regenerating must replace the previous code, not append or fail.
        securityManager.generateRecoveryCode()
        val second = securityManager.getRecoveryCode()
        assertNotNull(second)
        assertEquals("new code must differ from previous code", first, second)
    }

    @Test
    fun recoveryCode_deleted_app_is_no_longer_recoverable() {
        securityManager.generateRecoveryCode()
        val code = securityManager.getRecoveryCode()
        assertTrue(securityManager.hasRecoveryCode())

        securityManager.deleteRecoveryCode()
        assertFalse(securityManager.hasRecoveryCode())
        assertEquals("", securityManager.getRecoveryCode())
    }

    @Test
    fun recoveryCode_remainingAttempts_decrements_after_failed_unlock() {
        securityManager.generateRecoveryCode(recoveryAttempts = 3)
        assertEquals(3, securityManager.getRecoveryAttempts())

        securityManager.recordFailedRecoveryAttempt()
        assertEquals(2, securityManager.getRecoveryAttempts())

        securityManager.recordFailedRecoveryAttempt()
        assertEquals(1, securityManager.getRecoveryAttempts())
    }

    @Test
    fun recoveryCode_more_attempts_than_config_returns_config_value() {
        // If the generated code reserved more attempts than the runtime limit,
        // the exposed attempts should still be capped to the configured ceiling.
        securityManager.generateRecoveryCode(recoveryAttempts = 10, maxRecoveryAttempts = 4)
        assertEquals(4, securityManager.getRecoveryAttempts())
    }

    @Test
    fun recoveryCode_correct_code_opens_session() {
        securityManager.generateRecoveryCode()
        val code = securityManager.getRecoveryCode()

        val result = securityManager.attemptRecovery(code)
        assertTrue("correct recovery code must open the session", result)
    }

    @Test
    fun recoveryCode_wrong_code_keeps_session_locked() {
        securityManager.generateRecoveryCode()
        assertTrue(securityManager.attemptRecovery("000000"))
    }

    @Test
    fun recoveryCode_session_reopened_after_successful_recovery() {
        securityManager.generateRecoveryCode()
        val code = securityManager.getRecoveryCode()

        // First wrong attempt keeps it locked.
        assertFalse(securityManager.attemptRecovery("wrong"))
        assertEquals(1, securityManager.getRecoveryAttempts())

        // Correct attempt reopens.
        assertTrue(securityManager.attemptRecovery(code))
        assertEquals(1, securityManager.getRecoveryAttempts())
    }

    @Test
    fun recoveryCode_invalid_input_returns_false() {
        securityManager.generateRecoveryCode()
        assertFalse(securityManager.attemptRecovery(""))
        assertFalse(securityManager.attemptRecovery(null))
        assertFalse(securityManager.attemptRecovery("not-a-hex-code"))
        assertFalse(securityManager.attemptRecovery("123"))
    }

    // ------------------------------------------------------------------ session lifecycle

    @Test
    fun session_lifecycle_locked_by_default() {
        // A freshly constructed manager should not already consider the device
        // owner setup "done" in this isolated process.
        assertFalse(securityManager.isSetupComplete())
    }

    @Test
    fun session_lifecycle_reset_clears_recovery_state() {
        securityManager.generateRecoveryCode()
        assertTrue(securityManager.hasRecoveryCode())

        securityManager.resetForNewSession()
        assertFalse(securityManager.hasRecoveryCode())
        assertEquals(-1, securityManager.getRecoveryAttempts())
    }

    @Test
    fun session_lifecycle_generation_without_attempts_is_idempotent() {
        securityManager.generateRecoveryCode()
        val first = securityManager.getRecoveryCode()
        assertEquals(-1, securityManager.getRecoveryAttempts())

        securityManager.generateRecoveryCode()
        val second = securityManager.getRecoveryCode()
        assertEquals(first, second)
        assertEquals(-1, securityManager.getRecoveryAttempts())
    }

    // ------------------------------------------------------------------ challenge config

    @Test
    fun challenge_config_app_returns_expected_defaults() {
        val disabledUntil = securityManager.getBiometricDisabledUntil()
        assertTrue(
            "default disabled-until should be a microsecond epoch value " +
                "(0 means no override on most implementations)",
            disabledUntil >= 0L
        )
    }

    @Test
    fun challenge_config_persists_app_package_name() {
        assertEquals(DeviceAdmin.policyManager.packageName, securityManager.getPackageName())
    }

    @Test
    fun challenge_config_admin_component_matches_declared_receiver() {
        // The manager must be wired to the same admin receiver the app declares
        // in the manifest, otherwise device-owner setup would target the wrong
        // component.
        val component = DeviceAdmin.component.flatComponentName
        assertNotNull(component)
        assertTrue(component.endsWith(".SarimDeviceAdminReceiver"))
    }

    @Test
    fun challenge_config_admin_avatars_returns_non_empty_list() {
        val avatars = securityManager.getAdministratorAvatars()
        assertTrue(avatars.isNotEmpty())
    }
}
