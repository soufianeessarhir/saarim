package com.sarim.app.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Detection tests for the "Block Torrent apps" feature. Suspending what's already installed is the
 * Accessibility Service's job; this class covers the detection layer it relies on, whose cache is
 * the part that can silently go stale.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TorrentBlockTest {

    private lateinit var ctx: Context
    private lateinit var manager: BrowserRestrictionsManager

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        manager = BrowserRestrictionsManager(ctx)
        manager.invalidateTorrentCache()
    }

    /** Registers [packageName] as an installed app that handles magnet links. */
    private fun installMagnetHandler(packageName: String) {
        val shadowPm = Shadows.shadowOf(ctx.packageManager)
        val component = ComponentName(packageName, "$packageName.MainActivity")
        shadowPm.addActivityIfNotPresent(component)
        shadowPm.addIntentFilterForActivity(
            component,
            IntentFilter(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addDataScheme("magnet")
            }
        )
        manager.invalidateTorrentCache()
    }

    /** Registers [packageName] as an installed app that opens `.torrent` files. */
    private fun installTorrentFileHandler(packageName: String) {
        val shadowPm = Shadows.shadowOf(ctx.packageManager)
        val component = ComponentName(packageName, "$packageName.MainActivity")
        shadowPm.addActivityIfNotPresent(component)
        shadowPm.addIntentFilterForActivity(
            component,
            IntentFilter(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addDataScheme("content")
                addDataType("application/x-bittorrent")
            }
        )
        manager.invalidateTorrentCache()
    }

    @Test
    fun `nothing is detected before a handler is installed`() {
        assertEquals(emptySet<String>(), manager.getPossibleTorrentApps())
        assertFalse(manager.isTorrentApp("com.example.torrent"))
    }

    @Test
    fun `a magnet handler is detected`() {
        installMagnetHandler("com.example.torrent")

        assertTrue("com.example.torrent" in manager.getPossibleTorrentApps())
        assertTrue(manager.isTorrentApp("com.example.torrent"))
    }

    @Test
    fun `a dot-torrent file handler is detected`() {
        installTorrentFileHandler("com.example.tfile")

        assertTrue(manager.isTorrentApp("com.example.tfile"))
    }

    @Test
    fun `an app installed after the sweep is picked up once the cache is invalidated`() {
        installMagnetHandler("com.example.first")
        assertTrue(manager.isTorrentApp("com.example.first"))

        // Without the invalidation this stays invisible: the point of the test is that the
        // package-added broadcast must invalidate, or the sweep reports a stale list forever.
        installMagnetHandler("com.example.second")

        assertTrue(manager.isTorrentApp("com.example.second"))
        assertTrue(manager.isTorrentApp("com.example.first"))
    }

    @Test
    fun `unrelated apps are not detected`() {
        installMagnetHandler("com.example.torrent")

        assertFalse(manager.isTorrentApp("com.android.chrome"))
        assertFalse(manager.isTorrentApp("com.whatsapp"))
    }

    @Test
    fun `torrent detection and Tor detection stay independent`() {
        installMagnetHandler("com.example.torrent")

        // A torrent client is not a Tor component, and blocking one must not suspend the other.
        assertFalse(manager.isTorApp("com.example.torrent"))
        assertTrue(manager.isTorrentApp("com.example.torrent"))
    }
}
