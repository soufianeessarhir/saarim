package com.sarim.app.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarim.app.SarimApplication
import com.sarim.app.data.AppRepository
import com.sarim.app.data.VisualBlockingSettings
import com.sarim.app.models.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings screen state for visual blocking. The numbers are drafts until [saveSettings] is
 * called, so a slider moved by accident can be abandoned; the monitored-app list is saved as it
 * is changed, because that picker is already confirmed by its own dialog.
 */
class VisualBlockingViewModel : ViewModel() {
    private val context = SarimApplication.getInstance()
    private val prefs = context.getSharedPreferences(VisualBlockingSettings.PREFS_NAME, Context.MODE_PRIVATE)

    var targetPackages by mutableStateOf(
        prefs.getStringSet(VisualBlockingSettings.KEY_TARGET_PACKAGES, VisualBlockingSettings.DEFAULT_TARGET_PACKAGES)
            ?: VisualBlockingSettings.DEFAULT_TARGET_PACKAGES
    )
        private set

    var threshold by mutableFloatStateOf(
        prefs.getFloat(VisualBlockingSettings.KEY_THRESHOLD, VisualBlockingSettings.DEFAULT_THRESHOLD)
    )
        private set

    var suspendBlockCount by mutableIntStateOf(
        prefs.getInt(
            VisualBlockingSettings.KEY_SUSPEND_BLOCK_COUNT,
            VisualBlockingSettings.DEFAULT_SUSPEND_BLOCK_COUNT
        )
    )
        private set

    var suspendWindowMinutes by mutableIntStateOf(
        prefs.getInt(
            VisualBlockingSettings.KEY_SUSPEND_WINDOW_MINUTES,
            VisualBlockingSettings.DEFAULT_SUSPEND_WINDOW_MINUTES
        )
    )
        private set

    var suspendDurationMinutes by mutableIntStateOf(
        prefs.getInt(
            VisualBlockingSettings.KEY_SUSPEND_DURATION_MINUTES,
            VisualBlockingSettings.DEFAULT_SUSPEND_DURATION_MINUTES
        )
    )
        private set

    var apps by mutableStateOf<List<AppItem>>(emptyList())
        private set

    var isLoadingApps by mutableStateOf(false)
        private set

    init {
        loadApps()
    }

    fun toggleAppSelection(packageName: String) {
        val newSet = targetPackages.toMutableSet()
        if (!newSet.remove(packageName)) newSet.add(packageName)
        targetPackages = newSet
        prefs.edit { putStringSet(VisualBlockingSettings.KEY_TARGET_PACKAGES, targetPackages) }
    }

    fun updateThreshold(value: Float) {
        threshold = value
    }

    /**
     * The three settings below accept 0 while the user is clearing the field to type a new
     * number; [saveSettings] falls back to the default rather than persisting an unusable value.
     */
    fun updateSuspendBlockCount(value: Int) {
        suspendBlockCount = value
    }

    fun updateSuspendWindowMinutes(value: Int) {
        suspendWindowMinutes = value
    }

    fun updateSuspendDurationMinutes(value: Int) {
        suspendDurationMinutes = value
    }

    /** Persists every draft setting. Called from the screen's Save button. */
    fun saveSettings() {
        prefs.edit {
            putFloat(VisualBlockingSettings.KEY_THRESHOLD, threshold)
            putInt(
                VisualBlockingSettings.KEY_SUSPEND_BLOCK_COUNT,
                suspendBlockCount.takeIf { it >= 1 } ?: VisualBlockingSettings.DEFAULT_SUSPEND_BLOCK_COUNT
            )
            putInt(
                VisualBlockingSettings.KEY_SUSPEND_WINDOW_MINUTES,
                suspendWindowMinutes.takeIf { it >= 1 } ?: VisualBlockingSettings.DEFAULT_SUSPEND_WINDOW_MINUTES
            )
            putInt(
                VisualBlockingSettings.KEY_SUSPEND_DURATION_MINUTES,
                suspendDurationMinutes.takeIf { it >= 1 } ?: VisualBlockingSettings.DEFAULT_SUSPEND_DURATION_MINUTES
            )
        }
    }

    fun loadApps() {
        viewModelScope.launch {
            isLoadingApps = true
            apps = withContext(Dispatchers.IO) { AppRepository.getApps() }
            isLoadingApps = false
        }
    }
}
