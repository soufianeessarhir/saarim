package com.sarim.app.viewmodels

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthViewModelTest {

    @Test
    fun `starts locked and stays unlocked after authentication`() {
        val viewModel = AuthViewModel()

        assertFalse(viewModel.authenticated)
        viewModel.authenticate()
        assertTrue(viewModel.authenticated)
    }
}
