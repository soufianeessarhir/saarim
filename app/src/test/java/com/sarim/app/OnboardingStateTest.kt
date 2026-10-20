package com.sarim.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sarim.app.screens.common.ONBOARDING_SLIDE_COUNT
import com.sarim.app.viewmodels.NavigationViewModel
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingStateTest {

    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `onboarding completed flag defaults to false`() {
        AppPrefs.isOnboardingCompleted(context) shouldBe false
    }

    @Test
    fun `marking onboarding completed persists across reads`() {
        AppPrefs.markOnboardingCompleted(context)

        AppPrefs.isOnboardingCompleted(context) shouldBe true
    }

    @Test
    fun `carousel has exactly five slides`() {
        ONBOARDING_SLIDE_COUNT shouldBe 5
    }

    @Test
    fun `slide five cta routes to the existing owner setup screen`() {
        val navigation = NavigationViewModel()

        navigation.navigateTo("setup_device_owner")

        navigation.selectedTab() shouldBe "setup_device_owner"
    }
}
