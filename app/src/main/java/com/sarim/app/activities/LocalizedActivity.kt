package com.sarim.app.activities

import android.content.Context
import androidx.activity.ComponentActivity
import com.sarim.app.utils.LocaleUtils

/**
 * Base for every screen: applies the app's own language to the activity's resources.
 *
 * API 33+ resolves the per-app language for each component on its own. Below that the framework
 * only re-wraps AppCompat activities, and Sarim's screens are plain [ComponentActivity]s, so
 * Android 11 and 12 would otherwise keep showing the system language instead of the Arabic
 * default or a language picked in Config.
 */
abstract class LocalizedActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleUtils.localizedContext(newBase))
    }
}
