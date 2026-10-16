package com.sarim.app.utils

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.sarim.app.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for the app language.
 *
 * The app opens in Arabic; the phone's own language is only followed when the user picks one in
 * Config → Language or in the system's per-app language screen. These tests run under Robolectric
 * on both sides of the API 33 split (`LocaleManager` above, the wrapped resources below); the
 * machine's locale is en-US.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30, 34])
class LocaleUtilsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // _______________________________________________________________ the default

    @Test
    fun `the app applies the arabic default when it starts`() {
        // SarimApplication.onCreate ran before this test.
        assertEquals("ar", LocaleUtils.getLocale(context))
    }

    @Test
    fun `the arabic translations and rtl direction are the ones the screens resolve`() {
        val localized = LocaleUtils.localizedContext(context)

        assertEquals("ar", localized.resources.configuration.locales[0].language)
        assertEquals("التطبيقات", localized.getString(R.string.apps))
        assertEquals(View.LAYOUT_DIRECTION_RTL, localized.resources.configuration.layoutDirection)
    }

    @Test
    fun `the config picker reports arabic when the user never chose`() {
        val options = listOf("en", "pt", "ar")
        val selected = options.firstOrNull {
            LocaleUtils.getLocale(context).startsWith(it)
        } ?: LocaleUtils.DEFAULT_LANGUAGE

        assertEquals(LocaleUtils.DEFAULT_LANGUAGE, selected)
    }

    // _______________________________________________________________ the user's choice

    @Test
    fun `a language picked in config survives the next launch`() {
        LocaleUtils.setLocale(context, "en")
        LocaleUtils.applyDefaultLocale(context)

        assertEquals("en", LocaleUtils.getLocale(context))
        assertEquals("Apps", LocaleUtils.localizedContext(context).getString(R.string.apps))
    }

    @Test
    @Config(sdk = [34])
    fun `a per-app language set in system settings is kept on api 33 and up`() {
        context.getSystemService(LocaleManager::class.java)?.applicationLocales =
            LocaleList.forLanguageTags("pt")

        LocaleUtils.applyDefaultLocale(context)

        assertEquals("pt", LocaleUtils.getLocale(context))
        assertEquals(
            "pt",
            LocaleUtils.localizedContext(context).resources.configuration.locales[0].language
        )
    }
}
