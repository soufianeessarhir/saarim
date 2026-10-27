package com.sarim.app.screens.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarim.app.SarimAccessibilityService
import com.sarim.app.R
import com.sarim.app.screens.common.AppPickerDialog
import com.sarim.app.screens.common.RecoveryGateDialog
import com.sarim.app.screens.common.rememberRecoveryGate
import com.sarim.app.data.VisualBlockingSettings
import com.sarim.app.security.StrictnessLock
import com.sarim.app.viewmodels.VisualBlockingViewModel
import kotlinx.coroutines.delay

private const val DAY_MS = 24L * 60 * 60 * 1000

@Composable
fun VisualBlockingScreen(viewModel: VisualBlockingViewModel = viewModel()) {
    var showAppSelectionDialog by remember { mutableStateOf(false) }
    var showLockDialog by remember { mutableStateOf(false) }
    val recoveryGate = rememberRecoveryGate()
    val context = LocalContext.current

    val sessionActive = recoveryGate.isSessionActive
    val accessibilityActive = SarimAccessibilityService.isRunning

    // Recomputed on a ticker so the remaining time and the locked/unlocked state stay in step
    // while this screen is open — the lock can run out at any moment.
    var lockRemaining by remember { mutableLongStateOf(StrictnessLock.remainingMillis(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            lockRemaining = StrictnessLock.remainingMillis(context)
            // Second by second only matters while there is a countdown to show; an unlocked screen
            // just needs to notice a lock started elsewhere. Starting one from here sets the state
            // directly, so the slow tick is only a safety net.
            delay(if (lockRemaining > 0L) 1000L else 30_000L)
        }
    }

    val locked = lockRemaining > 0L
    // The switch, the categories and the sensitivity are exactly what the lock exists to pin, so
    // all of them freeze while it runs.
    val editable = sessionActive && !locked

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                stringResource(R.string.visual_blocking_explanation),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        item {
            val supporting = when {
                locked -> stringResource(
                    R.string.strictness_lock_remaining,
                    ((lockRemaining + DAY_MS - 1) / DAY_MS).toInt()
                )

                StrictnessLock.hasEverLocked(context) -> stringResource(R.string.strictness_lock_ended)
                else -> stringResource(R.string.strictness_lock_desc)
            }

            ListItem(
                headlineContent = {
                    Text(
                        stringResource(
                            if (locked) R.string.strictness_lock_locked_title
                            else R.string.strictness_lock
                        )
                    )
                },
                supportingContent = { Text(supporting) },
                leadingContent = {
                    Icon(
                        if (locked) Icons.Outlined.LockClock else Icons.Outlined.LockOpen,
                        null
                    )
                },
                trailingContent = {
                    if (!locked) {
                        Button(onClick = { showLockDialog = true }) {
                            Text(stringResource(R.string.strictness_lock_start))
                        }
                    }
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        if (!sessionActive) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.visual_blocking_locked)) },
                    leadingContent = { Icon(Icons.Outlined.Lock, null) },
                    modifier = Modifier.clickable { recoveryGate.run {} }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.visual_blocking_enable)) },
                supportingContent = {
                    when {
                        locked -> Text(stringResource(R.string.strictness_lock_pinned))
                        !accessibilityActive -> Text(
                            stringResource(R.string.visual_blocking_requires_accessibility),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                leadingContent = { Icon(Icons.Outlined.ImageSearch, null) },
                trailingContent = {
                    Switch(
                        checked = viewModel.enabled,
                        enabled = editable && accessibilityActive,
                        onCheckedChange = { viewModel.updateEnabled(it) }
                    )
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.visual_blocking_apps_section)) },
                supportingContent = {
                    val count = viewModel.targetPackages.size
                    Text(
                        if (count == 0) stringResource(R.string.no_apps)
                        else stringResource(R.string.visual_blocking_apps_selected, count)
                    )
                },
                trailingContent = {
                    Button(
                        enabled = editable,
                        onClick = { showAppSelectionDialog = true }
                    ) {
                        Text(stringResource(R.string.select_apps))
                    }
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        item {
            Text(
                stringResource(R.string.visual_blocking_categories_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Text(
                stringResource(R.string.visual_blocking_categories_desc),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        items(VisualBlockingSettings.SELECTABLE_CATEGORIES) { category ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = editable) { viewModel.toggleCategory(category) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = viewModel.selectedCategories.contains(category),
                    enabled = editable,
                    onCheckedChange = { viewModel.toggleCategory(category) }
                )
                Text(categoryLabel(category), modifier = Modifier.padding(start = 8.dp))
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }

        item {
            Text(
                stringResource(R.string.visual_blocking_threshold_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Text(
                stringResource(R.string.visual_blocking_threshold_desc),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = viewModel.threshold,
                    enabled = editable,
                    onValueChange = { viewModel.updateThreshold(it) },
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${(viewModel.threshold * 100).toInt()}%",
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }

        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.visual_blocking_suspend)) },
                supportingContent = { Text(stringResource(R.string.visual_blocking_suspend_desc)) },
                leadingContent = { Icon(Icons.Outlined.Timer, null) },
                trailingContent = {
                    Checkbox(
                        checked = viewModel.suspendEnabled,
                        enabled = sessionActive,
                        onCheckedChange = { viewModel.updateSuspendEnabled(it) }
                    )
                },
                modifier = Modifier.clickable(enabled = sessionActive) {
                    viewModel.updateSuspendEnabled(!viewModel.suspendEnabled)
                }
            )

            if (viewModel.suspendEnabled) {
                NumberSettingField(
                    label = stringResource(R.string.visual_blocking_suspend_count),
                    supportingText = stringResource(R.string.visual_blocking_suspend_count_desc),
                    value = viewModel.suspendBlockCount,
                    enabled = sessionActive,
                    onValueChange = { viewModel.updateSuspendBlockCount(it) }
                )
                NumberSettingField(
                    label = stringResource(R.string.visual_blocking_suspend_window),
                    supportingText = stringResource(R.string.visual_blocking_suspend_window_desc),
                    value = viewModel.suspendWindowMinutes,
                    enabled = sessionActive,
                    onValueChange = { viewModel.updateSuspendWindowMinutes(it) }
                )
                NumberSettingField(
                    label = stringResource(R.string.visual_blocking_suspend_duration),
                    supportingText = stringResource(R.string.visual_blocking_suspend_duration_desc),
                    value = viewModel.suspendDurationMinutes,
                    enabled = sessionActive,
                    onValueChange = { viewModel.updateSuspendDurationMinutes(it) }
                )
                Text(
                    stringResource(
                        R.string.visual_blocking_suspend_summary,
                        viewModel.suspendBlockCount,
                        viewModel.suspendWindowMinutes,
                        viewModel.suspendDurationMinutes
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAppSelectionDialog) {
        AppPickerDialog(
            apps = viewModel.apps,
            isLoading = viewModel.isLoadingApps,
            isSelected = { viewModel.targetPackages.contains(it) },
            onToggle = { viewModel.toggleAppSelection(it) },
            onDismiss = { showAppSelectionDialog = false }
        )
    }

    if (showLockDialog) {
        StrictnessLockDialog(
            hasTargetApps = viewModel.targetPackages.isNotEmpty(),
            accessibilityActive = accessibilityActive,
            onStart = { days ->
                StrictnessLock.start(context, days)
                StrictnessLock.enforce(context)
                lockRemaining = StrictnessLock.remainingMillis(context)
                showLockDialog = false
            },
            onDismiss = { showLockDialog = false }
        )
    }

    RecoveryGateDialog(recoveryGate)
}

/**
 * Duration picker + a plain description of what starting the lock does. Nothing here is gated by
 * the recovery code: the lock only ever makes the app stricter, and the confirmation text is what
 * makes it a deliberate act.
 */
@Composable
private fun StrictnessLockDialog(
    hasTargetApps: Boolean,
    accessibilityActive: Boolean,
    onStart: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var daysText by remember { mutableStateOf(StrictnessLock.DEFAULT_DURATION_DAYS.toString()) }
    val days = daysText.toIntOrNull() ?: 0
    val validDuration = days >= StrictnessLock.MIN_DURATION_DAYS

    // Locking in a block that cannot do anything would only trap the user who set it: with no
    // monitored app, or with accessibility off, visual blocking detects nothing at all.
    val blocker = when {
        !accessibilityActive -> R.string.visual_blocking_requires_accessibility
        !hasTargetApps -> R.string.strictness_lock_requires_apps
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.LockClock, null) },
        title = { Text(stringResource(R.string.strictness_lock_duration_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(
                        R.string.strictness_lock_duration_text,
                        StrictnessLock.MIN_DURATION_DAYS
                    )
                )
                Text(stringResource(R.string.strictness_lock_what_changes))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StrictnessLock.DURATION_PRESETS_DAYS.forEach { preset ->
                        FilterChip(
                            selected = days == preset,
                            onClick = { daysText = preset.toString() },
                            label = { Text(preset.toString()) }
                        )
                    }
                }
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 5) daysText = input
                    },
                    label = { Text(stringResource(R.string.strictness_lock_days_label)) },
                    isError = !validDuration,
                    supportingText = {
                        Text(
                            stringResource(
                                R.string.strictness_lock_days_invalid,
                                StrictnessLock.MIN_DURATION_DAYS
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                if (blocker != null) {
                    Text(
                        stringResource(blocker),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = validDuration && blocker == null,
                onClick = { onStart(days) }
            ) {
                Text(stringResource(R.string.strictness_lock_start_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/**
 * Label + numeric text field row used by the "suspend app" settings. The field is allowed to go
 * empty while the user retypes a number — [onValueChange] then reports 0, which the view model
 * treats as "not a usable value yet" and doesn't persist.
 */
@Composable
private fun NumberSettingField(
    label: String,
    supportingText: String,
    value: Int,
    enabled: Boolean,
    onValueChange: (Int) -> Unit
) {
    var textFieldValue by remember { mutableStateOf(value.toString()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(supportingText, style = MaterialTheme.typography.bodySmall)
        }
        OutlinedTextField(
            value = textFieldValue,
            enabled = enabled,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                    textFieldValue = newValue
                    onValueChange(newValue.toIntOrNull() ?: 0)
                }
            },
            modifier = Modifier
                .width(96.dp)
                .padding(start = 8.dp),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
    }
}

@Composable
private fun categoryLabel(category: String): String = when (category) {
    "drawings" -> stringResource(R.string.nsfw_category_drawings)
    "hentai" -> stringResource(R.string.nsfw_category_hentai)
    "porn" -> stringResource(R.string.nsfw_category_porn)
    "sexy" -> stringResource(R.string.nsfw_category_sexy)
    else -> category
}

