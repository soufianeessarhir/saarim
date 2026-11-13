package com.sarim.app.screens.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.Adb
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.NoAdultContent
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarim.app.R
import com.sarim.app.SarimAccessibilityService
import com.sarim.app.security.CommitmentLock
import com.sarim.app.viewmodels.DeviceOwnerViewModel
import com.sarim.app.viewmodels.NavigationViewModel
import kotlinx.coroutines.delay

/**
 * The landing tab: one glance at whether protection is running, the commitment, the two
 * prerequisites that can be missing, and every always-on layer. It adds no new enforcement —
 * [com.sarim.app.security.MandatoryProtection] still owns that — it only makes the state visible
 * before the user opens Protection or Config.
 */
@Composable
fun HomeTab(
    deviceOwnerViewModel: DeviceOwnerViewModel = viewModel(),
    navViewModel: NavigationViewModel = viewModel()
) {
    val context = LocalContext.current
    val isOwner = deviceOwnerViewModel.isDeviceOwner()
    val advancedBlocking = SarimAccessibilityService.isRunning

    var remaining by remember { mutableLongStateOf(CommitmentLock.remainingMillis(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            remaining = CommitmentLock.remainingMillis(context)
            delay(60_000)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            ProtectionHero(active = isOwner && advancedBlocking)
        }

        item {
            CommitmentCard(
                permanent = CommitmentLock.isPermanent(context),
                regular = CommitmentLock.isRegular(context),
                stopped = CommitmentLock.isStopped(context),
                remaining = remaining,
                started = CommitmentLock.hasStarted(context)
            )
        }

        item { SectionTitle(stringResource(R.string.home_status_title)) }

        item {
            CheckRow(
                icon = Icons.Outlined.Adb,
                title = stringResource(R.string.device_owner_state),
                ok = isOwner,
                onClick = { navViewModel.navigateTo(NavigationViewModel.SCREEN_SETUP_DEVICE_OWNER) }
            )
        }
        item {
            CheckRow(
                icon = Icons.Outlined.Accessibility,
                title = stringResource(R.string.advanced_blocking),
                ok = advancedBlocking,
                onClick = { navViewModel.navigateTo(NavigationViewModel.SCREEN_CONFIG) }
            )
        }

        item { SectionTitle(stringResource(R.string.protection_layers_title)) }

        item {
            HomeLayerRow(
                icon = Icons.Outlined.ImageSearch,
                title = stringResource(R.string.visual_blocking),
                onClick = { navViewModel.navigateTo(NavigationViewModel.SCREEN_VISUAL_BLOCKING) }
            )
        }
        item {
            HomeLayerRow(
                icon = Icons.Outlined.NoAdultContent,
                title = stringResource(R.string.blocked_words_feat),
                onClick = { navViewModel.navigateTo(NavigationViewModel.SCREEN_BLOCKED_WORDS) }
            )
        }
        item {
            HomeLayerRow(
                icon = Icons.Outlined.Web,
                title = stringResource(R.string.browser_restrictions),
                onClick = { navViewModel.navigateTo(NavigationViewModel.SCREEN_BROWSER_RESTRICTIONS) }
            )
        }
        item {
            HomeLayerRow(
                icon = Icons.Outlined.Block,
                title = stringResource(R.string.system_restrictions),
                onClick = { navViewModel.navigateTo(NavigationViewModel.SCREEN_SYSTEM_RESTRICTIONS) }
            )
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ProtectionHero(active: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.errorContainer,
            contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.Security,
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(
                    if (active) R.string.protection_active_title
                    else R.string.protection_incomplete_title
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(
                    if (active) R.string.home_active_body else R.string.home_incomplete_body
                ),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CommitmentCard(
    permanent: Boolean,
    regular: Boolean,
    stopped: Boolean,
    remaining: Long,
    started: Boolean
) {
    val active = started && remaining > 0L
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(
                    if (active || permanent) R.string.commitment_active_title
                    else R.string.commitment_title
                ),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    !started -> stringResource(R.string.commitment_setup_title)
                    regular -> stringResource(R.string.commitment_regular_status)
                    stopped -> stringResource(R.string.commitment_stopped_status)
                    permanent -> stringResource(R.string.commitment_remaining_permanent)
                    active -> {
                        val daysLeft = (remaining / (24L * 60L * 60L * 1000L)).toInt()
                        pluralStringResource(R.plurals.commitment_remaining_days, daysLeft, daysLeft)
                    }
                    else -> stringResource(R.string.commitment_ended)
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(
                    if (regular) R.string.commitment_regular_desc
                    else R.string.commitment_pinned_desc
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun CheckRow(
    icon: ImageVector,
    title: String,
    ok: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (ok) Icons.Filled.CheckCircle else Icons.Outlined.Error,
                    contentDescription = null,
                    tint = if (ok) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
                Text(
                    stringResource(if (ok) R.string.granted else R.string.not_granted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (ok) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun HomeLayerRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(stringResource(R.string.always_on)) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
