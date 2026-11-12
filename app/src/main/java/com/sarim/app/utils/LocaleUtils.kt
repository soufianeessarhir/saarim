package com.sarim.app.utils

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleUtils {

    /** Language the app opens in before the user picks another one in Config → Language. */
    const val DEFAULT_LANGUAGE = "ar"

    private const val PREFS_NAME = "sarim_locale"
    private const val KEY_LANGUAGE = "app_language"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Applies the language chosen in Config → Language, and remembers it.
     *
     * Returns true when the caller must recreate its activity to see the change. On API 33+
     * `LocaleManager` triggers the configuration change itself; below that the app is not an
     * `AppCompatActivity`, so AppCompat's locale record is not applied to the current activity.
     */
    fun setLocale(context: Context, languageCode: String): Boolean {
        prefs(context).edit { putString(KEY_LANGUAGE, languageCode) }
        applyLocale(context, languageCode)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    }

    /**
     * Gives the app its own language on the first launch, so a fresh install opens in
     * [DEFAULT_LANGUAGE] even when the phone is set to something else.
     *
     * A per-app language the user already chose in the system settings counts as their decision
     * and is kept instead of being overwritten.
     */
    fun applyDefaultLocale(context: Context) {
        val prefs = prefs(context)
        if (prefs.contains(KEY_LANGUAGE)) return
        val system = systemPerAppLocale(context)
        val language = system.ifEmpty { DEFAULT_LANGUAGE }
        // Committed, not applied: on API 33+ setting a locale can restart the process, and a
        // half-written preference would send the next launch through this branch again.
        prefs.edit(commit = true) { putString(KEY_LANGUAGE, language) }
        if (language != system) applyLocale(context, language)
    }

    /** Language the app is displayed in: the system's per-app pick, else the stored one. */
    fun getLocale(context: Context): String =
        systemPerAppLocale(context).ifEmpty {
            prefs(context).getString(KEY_LANGUAGE, null) ?: Locale.getDefault().language
        }

    /**
     * Context whose resources follow the app language.
     *
     * API 33+ resolves the per-app language for every component by itself; below that the
     * framework only re-wraps AppCompat activities, and Sarim's screens are plain
     * `ComponentActivity`s, so each one asks for the configuration it should use. Notifications
     * use the same call for their channel names and text.
     */
    fun localizedContext(context: Context): Context {
        val locale = Locale.forLanguageTag(getLocale(context))
        val config = Configuration(context.resources.configuration).apply { setLocale(locale) }
        return context.createConfigurationContext(config)
    }

    /** Per-app language the system holds for Sarim, empty on releases without one. */
    private fun systemPerAppLocale(context: Context): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // A locale list can hold more than one language; the app only ever sets one, and
            // feeding a comma-separated tag to Locale.forLanguageTag would produce nonsense.
            context.getSystemService(LocaleManager::class.java)?.applicationLocales
                ?.get(0)?.toLanguageTag().orEmpty()
        } else {
            ""
        }

    private fun applyLocale(context: Context, languageCode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                LocaleList.forLanguageTags(languageCode)
        } else {
            // No LocaleManager on API 30-32: AppCompat keeps the record and the screens read
            // their language from [localizedContext].
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
        }
    }
}
