package com.sarim.app

import android.content.Context
import androidx.core.content.edit

object AppPrefs {

    private const val PREFS_ONBOARDING = "onboarding_prefs"
    private const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"
    private const val PROTECTION_SETUP_CONFIRMED_KEY = "protection_setup_confirmed"

    fun isOnboardingCompleted(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_ONBOARDING, Context.MODE_PRIVATE)
            .getBoolean(ONBOARDING_COMPLETED_KEY, false)
    }

    fun markOnboardingCompleted(context: Context) {
        context.getSharedPreferences(PREFS_ONBOARDING, Context.MODE_PRIVATE).edit {
            putBoolean(ONBOARDING_COMPLETED_KEY, true)
        }
    }

    /**
     * Whether the user has reviewed and confirmed the protection stack on first run. It has to be
     * confirmed before the commitment can be started; installs that already carry a commitment are
     * treated as confirmed, so an upgrade does not send anyone back through setup.
     */
    fun isProtectionSetupConfirmed(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_ONBOARDING, Context.MODE_PRIVATE)
            .getBoolean(PROTECTION_SETUP_CONFIRMED_KEY, false)
    }

    fun markProtectionSetupConfirmed(context: Context) {
        context.getSharedPreferences(PREFS_ONBOARDING, Context.MODE_PRIVATE).edit {
            putBoolean(PROTECTION_SETUP_CONFIRMED_KEY, true)
        }
    }
}
