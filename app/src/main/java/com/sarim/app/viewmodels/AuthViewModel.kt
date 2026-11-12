package com.sarim.app.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * Holds whether the current app session passed the lock screen.
 *
 * A `ViewModel` survives activity recreation (rotation, locale changes) but not process death, so
 * changing a language does not demand the password again while a cold launch still starts locked.
 */
class AuthViewModel : ViewModel() {

    var authenticated by mutableStateOf(false)
        private set

    fun authenticate() {
        authenticated = true
    }
}
