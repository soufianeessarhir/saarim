package com.sarim.app.viewmodels

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import com.sarim.app.data.DeviceAdmin
import com.sarim.app.data.SystemRestrictions

class RestrictionsViewModel : ViewModel() {
    private val dpm = DeviceAdmin.policyManager
    private val adminName = DeviceAdmin.component

    // Current state in the UI (draft)
    val draftRestrictions = mutableStateMapOf<String, Boolean>()

    // Actual state applied in the system
    val appliedRestrictions = mutableStateMapOf<String, Boolean>()

    val recommendedKeys get() = SystemRestrictions.recommendedKeys
    private val managedKeys get() = SystemRestrictions.allKeys

    fun resourceNameFor(key: String): String? = SystemRestrictions.resourceNameFor(key)

    init {
        loadRestrictions()
    }

    fun loadRestrictions() {
        if (!dpm.isDeviceOwnerApp(adminName.packageName)) return
        val current = dpm.getUserRestrictions(adminName)
        managedKeys.forEach { key ->
            // The mandatory ones are always reported as on, even before the platform has
            // acknowledged them, because [MandatoryProtection] re-applies them on every launch.
            val enabled = key in SystemRestrictions.mandatoryKeys || current.getBoolean(key)
            draftRestrictions[key] = enabled
            appliedRestrictions[key] = enabled
        }
    }

    fun toggleDraft(key: String, enabled: Boolean) {
        draftRestrictions[key] = enabled
    }

    fun toggleAllDrafts(keys: List<String>, enabled: Boolean) {
        keys.forEach { key -> draftRestrictions[key] = enabled }
    }

    fun applyChanges() {
        managedKeys.forEach { key ->
            val shouldEnable = key in SystemRestrictions.mandatoryKeys || draftRestrictions[key] == true
            if (shouldEnable) {
                dpm.addUserRestriction(adminName, key)
            } else {
                dpm.clearUserRestriction(adminName, key)
            }
        }
        loadRestrictions()
    }

    fun isAllDraftsEnabled(keys: List<String>): Boolean = keys.all { draftRestrictions[it] == true }

    fun hasPendingChanges(): Boolean = managedKeys.any { draftRestrictions[it] != appliedRestrictions[it] }
}
