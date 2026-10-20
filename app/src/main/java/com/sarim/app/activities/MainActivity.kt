package com.sarim.app.activities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.AppBlocking
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarim.app.R
import com.sarim.app.data.ApkUpdateInstaller
import com.sarim.app.data.UsageWarningSettings
import com.sarim.app.screens.setup.SetupDeviceOwnerPrivileges
import com.sarim.app.screens.setup.SetupRecovery
import com.sarim.app.screens.tabs.*
import com.sarim.app.security.SecurityManager
import com.sarim.app.ui.theme.SarimTheme
import com.sarim.app.viewmodels.DeviceOwnerViewModel
import com.sarim.app.AppPrefs
import com.sarim.app.screens.common.OnboardingCarousel
import com.sarim.app.viewmodels.NavigationViewModel
import kotlinx.coroutines.delay

class MainActivity : LocalizedActivity() {

    private val authenticated = mutableStateOf(false)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SecurityManager.consumeDebugAutoStartSession(this)) authenticated.value = true
        SecurityManager.consumeDebugRestoreUnknownSourcesRestriction(this)
        ApkUpdateInstaller.restoreUnknownSourcesRestrictionsIfIdle(this)
        enableEdgeToEdge()
        setContent {
            SarimTheme {
                val viewModel: DeviceOwnerViewModel = viewModel()
                viewModel.addShizukuListener()
                val navViewModel: NavigationViewModel = viewModel()

                var onboardingCompleted by remember {
                    mutableStateOf(AppPrefs.isOnboardingCompleted(this))
                }

                val currentScreen = navViewModel.selectedTab()
                val isRoot = currentScreen in listOf("restrictions", "apps", "config")

                BackHandler(enabled = !isRoot) {
                    navViewModel.goBack()
                }

                var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
                LaunchedEffect(Unit) {
                    while (true) {
                        currentTime = System.currentTimeMillis()
                        delay(1000)
                    }
                }

                val context = LocalContext.current
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) {}
                LaunchedEffect(authenticated.value) {
                    if (!authenticated.value) return@LaunchedEffect
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect

                    val warningPrefs = context.getSharedPreferences(UsageWarningSettings.PREFS_NAME, Context.MODE_PRIVATE)
                    val warningEnabled = warningPrefs.getBoolean(
                        UsageWarningSettings.KEY_ENABLED, UsageWarningSettings.DEFAULT_ENABLED
                    )
                    if (!warningEnabled) return@LaunchedEffect

                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.app_name)) },
                            navigationIcon = {
                                if (!isRoot) {
                                    IconButton(onClick = { navViewModel.goBack() }) {
                                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                                    }
                                }
                            },
                            actions = {
                                if (SecurityManager.isSessionActive()) {
                                    val remaining = SecurityManager.sessionEndTime - currentTime
                                    val minutes = (remaining / 1000) / 60
                                    val seconds = (remaining / 1000) % 60

                                    // Session countdown as a compact pill, then end-of-session button.
                                    Surface(
                                        shape = MaterialTheme.shapes.extraLarge,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(
                                                horizontal = 10.dp, vertical = 4.dp
                                            )
                                        ) {
                                            Icon(
                                                Icons.Outlined.LockClock,
                                                contentDescription = null,
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                            Text(
                                                text = "%02d:%02d".format(minutes, seconds),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                    IconButton(onClick = { SecurityManager.renewSession() }) {
                                        Icon(Icons.Outlined.Refresh, stringResource(R.string.renew_session))
                                    }
                                    IconButton(onClick = { SecurityManager.endSession() }) {
                                        Icon(Icons.AutoMirrored.Outlined.Logout, null)
                                    }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        if (!SecurityManager.isRecoveryCodeSet(this) || !authenticated.value) return@Scaffold

                        val tabs = listOf(
                            Pair("restrictions", stringResource(R.string.restrictions)),
                            Pair("apps", stringResource(R.string.apps)),
                            Pair("config", stringResource(R.string.config))
                        )

                        val selectedBaseTab = when (currentScreen) {
                            "restrictions" -> "restrictions"
                            "apps" -> "apps"
                            "config", "setup_device_owner", "activity_blocker", "browser_restrictions", "blocked_words", "visual_blocking", "impulse_lock", "focus_mode", "usage_warning", "color_filters" -> "config"
                            else -> "restrictions"
                        }

                        NavigationBar {
                            tabs.forEach { pair ->
                                NavigationBarItem(
                                    selected = selectedBaseTab == pair.first,
                                    onClick = { navViewModel.navigateTo(pair.first) },
                                    label = { Text(pair.second) },
                                    icon = {
                                        Icon(
                                            when (pair.first) {
                                                "restrictions" -> Icons.Outlined.Block
                                                "apps" -> Icons.Default.AppBlocking
                                                else -> Icons.Outlined.Settings
                                            }, contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->

                    if (!(SecurityManager.isRecoveryCodeSet(this)))
                        SetupRecovery(innerPadding)
                    else {
                        if (!authenticated.value)
                            Box(
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .consumeWindowInsets(innerPadding)
                            ) {
                                LockScreen(onAuthenticated = { authenticated.value = true })
                            }
                        else if (!onboardingCompleted)
                            OnboardingCarousel(
                                onFinish = {
                                    AppPrefs.markOnboardingCompleted(this)
                                    onboardingCompleted = true
                                }
                            )
                        else
                            Box(modifier = Modifier.padding(innerPadding)) {
                                when (currentScreen) {
                                    "restrictions" -> RestrictionsTab()
                                    "apps" -> AppsTab()
                                    "config" -> ConfigTab()
                                    "setup_device_owner" -> SetupDeviceOwnerPrivileges()
                                    "activity_blocker" -> ActivityBlockerScreen()
                                    "browser_restrictions" -> BrowserRestrictionsScreen()
                                    "blocked_words" -> BlockedWordsScreen()
                                    "visual_blocking" -> VisualBlockingScreen()
                                    "impulse_lock" -> ImpulseLockScreen()
                                    "focus_mode" -> FocusModeScreen()
                                    "usage_warning" -> UsageWarningScreen()
                                    "color_filters" -> ColorFilterScreen()
                                }
                            }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SecurityManager.endSession()
    }
}
