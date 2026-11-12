package com.sarim.app

import android.content.Context
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking

/**
 * The Application enforces the protection stack on its own IO scope at launch. A test that clears
 * and writes preferences has to let that pass finish first: otherwise a late pass can read the
 * cleared state and overwrite what the test just wrote.
 */
fun awaitApplicationLaunchWork(context: Context) {
    val app = context.applicationContext as? SarimApplication ?: return
    runBlocking {
        app.applicationScope.coroutineContext[Job]
            ?.children
            ?.toList()
            ?.forEach { it.join() }
    }
}
