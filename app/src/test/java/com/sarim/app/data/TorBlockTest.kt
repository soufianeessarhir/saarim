package com.sarim.app.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Detection tests for the "Block Tor / Orbot" feature. The point of the identity matching is that
 * the *official* package ids are not the only thing that gets caught: a rebuilt or rebranded Tor
 * client has to be recognized too, otherwise renaming the APK is a one-step bypass.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TorBlockTest {

    private lateinit var ctx: Context
    private lateinit var manager: BrowserRestrictionsManager

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        manager = BrowserRestrictionsManager(ctx)
    }

    /** Registers [packageName] as installed, with an optional user-visible [label]. */
    private fun install(packageName: String, label: String? = null) {
        val appInfo = ApplicationInfo().apply {
            this.packageName = packageName
            if (label != null) nonLocalizedLabel = label
        }
        Shadows.shadowOf(ctx.packageManager).installPackage(
            PackageInfo().apply {
                this.packageName = packageName
                applicationInfo = appInfo
            }
        )
        manager.invalidateTorCache()
    }

    @Test
    fun `official Tor and Orbot package ids are detected`() {
        assertTrue(manager.isTorApp("org.torproject.torbrowser"))
        assertTrue(manager.isTorApp("org.torproject.torbrowser_alpha"))
        assertTrue(manager.isTorApp("org.torproject.android")) // Orbot
        assertTrue(manager.isTorApp("org.torproject.torservices"))
        assertTrue(manager.isTorApp("info.guardianproject.orfox"))
    }

    @Test
    fun `any app published under the torproject id is detected`() {
        // Not in the explicit list, but the publisher id gives it away — this is what makes a
        // future Tor Project app blocked without a code change.
        assertTrue(manager.isTorApp("org.torproject.vpn"))
        assertTrue(manager.isTorApp("org.torproject.somethingnew"))
    }

    @Test
    fun `rebuilt or rebranded Tor clients are detected by identity`() {
        // Different package ids, same purpose. Separators must not matter.
        assertTrue(manager.isTorApp("com.example.torbrowser"))
        assertTrue(manager.isTorApp("com.foo.tor_browser"))
        assertTrue(manager.isTorApp("net.orbot.proxy"))
        assertTrue(manager.isTorApp("com.someone.onionbrowser"))
    }

    @Test
    fun `an installed app is detected by its label even when its id gives nothing away`() {
        install("com.example.fastbrowser", label = "Tor Browser")

        assertTrue(manager.isTorApp("com.example.fastbrowser"))
        assertTrue("com.example.fastbrowser" in manager.getPossibleTorApps())
    }

    @Test
    fun `an installed app with an innocuous id and label is not detected`() {
        install("com.example.notes", label = "Notes")

        assertFalse(manager.isTorApp("com.example.notes"))
        assertFalse("com.example.notes" in manager.getPossibleTorApps())
    }

    @Test
    fun `the installed-app sweep refreshes after the cache is invalidated`() {
        install("org.torproject.torbrowser")
        assertTrue("org.torproject.torbrowser" in manager.getPossibleTorApps())

        // A Tor app that appears after the sweep above must be picked up by the next one.
        install("com.example.fastbrowser", label = "Orbot")

        val swept = manager.getPossibleTorApps()
        assertTrue("com.example.fastbrowser" in swept)
        assertTrue("org.torproject.torbrowser" in swept)
    }

    @Test
    fun `unrelated packages are not detected`() {
        assertFalse(manager.isTorApp("com.android.chrome"))
        assertFalse(manager.isTorApp("org.mozilla.firefox"))
        assertFalse(manager.isTorApp("com.whatsapp"))
        // Contains "tor" as a substring but is a game.
        assertFalse(manager.isTorApp("com.example.torque"))
    }

    @Test
    fun `browser detection is not confused with Tor detection`() {
        // Tor Browser does not register as CATEGORY_APP_BROWSER, so the two checks must stay
        // independent — otherwise "Block Tor" would start suspending ordinary browsers.
        install("com.android.chrome", label = "Chrome")
        assertFalse(manager.isTorApp("com.android.chrome"))
    }
}
