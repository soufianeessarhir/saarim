package com.sarim.app

import android.content.Context
import androidx.core.content.edit

object AppPrefs {

    private const val PREFS_ONBOARDING = "onboarding_prefs"
    private const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"

    fun isOnboardingCompleted(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_ONBOARDING, Context.MODE_PRIVATE)
            .getBoolean(ONBOARDING_COMPLETED_KEY, false)
    }

    fun markOnboardingCompleted(context: Context) {
        context.getSharedPreferences(PREFS_ONBOARDING, Context.MODE_PRIVATE).edit {
            putBoolean(ONBOARDING_COMPLETED_KEY, true)
        }
    }
}
