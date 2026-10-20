package com.sarim.app.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarim.app.R
import com.sarim.app.viewmodels.NavigationViewModel
import kotlinx.coroutines.launch

/** Number of slides in [OnboardingCarousel]; the pager, the dots and the next/finish logic all use it. */
const val ONBOARDING_SLIDE_COUNT = 5

private data class OnboardingSlideContent(
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int,
    val ctaRes: Int? = null
)

private val slides = listOf(
    OnboardingSlideContent(
        icon = Icons.Filled.Security,
        titleRes = R.string.onboarding_slide_0_title,
        bodyRes = R.string.onboarding_slide_0_body
    ),
    OnboardingSlideContent(
        icon = Icons.Filled.Timer,
        titleRes = R.string.onboarding_slide_1_title,
        bodyRes = R.string.onboarding_slide_1_body
    ),
    OnboardingSlideContent(
        icon = Icons.Filled.PushPin,
        titleRes = R.string.onboarding_slide_2_title,
        bodyRes = R.string.onboarding_slide_2_body
    ),
    OnboardingSlideContent(
        icon = Icons.Filled.Timer,
        titleRes = R.string.onboarding_slide_3_title,
        bodyRes = R.string.onboarding_slide_3_body
    ),
    OnboardingSlideContent(
        icon = Icons.Filled.Settings,
        titleRes = R.string.onboarding_slide_4_title,
        bodyRes = R.string.onboarding_slide_4_body,
        ctaRes = R.string.onboarding_slide_4_cta
    )
)

/**
 * "How it works" tour. Shown once on first run and re-opened from the Config tab, so it is a
 * full-screen dialog on both paths. [onFinish] is called by Skip and by Got it; the caller owns
 * the one-time flag.
 */
@Composable
fun OnboardingCarousel(
    onFinish: () -> Unit,
    viewModel: NavigationViewModel = viewModel()
) {
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_SLIDE_COUNT })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == ONBOARDING_SLIDE_COUNT - 1

    Dialog(
        onDismissRequest = onFinish,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    OnboardingSlide(slides[page], onSetupClick = {
                        onFinish()
                        viewModel.navigateTo("setup_device_owner")
                    })
                }

                if (!isLastPage) {
                    TextButton(
                        onClick = onFinish,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                    ) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(ONBOARDING_SLIDE_COUNT) { index ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (index == pagerState.currentPage) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        },
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (isLastPage) {
                                onFinish()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            stringResource(
                                if (isLastPage) R.string.got_it else R.string.onboarding_next
                            )
                        )
                        if (!isLastPage) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingSlide(slide: OnboardingSlideContent, onSetupClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(top = 72.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = slide.icon,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(slide.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(slide.bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (slide.ctaRes != null) {
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onSetupClick) {
                Text(stringResource(slide.ctaRes))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.onboarding_slide_4_note),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
