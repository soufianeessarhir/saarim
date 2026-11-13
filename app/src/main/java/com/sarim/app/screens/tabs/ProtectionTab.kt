package com.sarim.app.screens.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarim.app.R
import com.sarim.app.SarimAccessibilityService
import com.sarim.app.viewmodels.DeviceOwnerViewModel
import com.sarim.app.viewmodels.NavigationViewModel

/**
 * The layer overview: what is protected right now, and the one place that says out loud whether a
 * prerequisite is missing. Every layer listed here is enforced by
 * [com.sarim.app.security.MandatoryProtection]; none of them has an off switch. The commitment and
 * the overall status live on the Home tab.
 */
@Composable
fun ProtectionTab(
    deviceOwnerViewModel: DeviceOwnerViewModel = viewModel(),
    navViewModel: NavigationViewModel = viewModel()
) {
    val isOwner = deviceOwnerViewModel.isDeviceOwner()
    val advancedBlocking = SarimAccessibilityService.isRunning

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (!isOwner || !advancedBlocking) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.protection_incomplete_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        if (!isOwner) {
                            Text(stringResource(R.string.protection_needs_owner))
                            Text(
                                stringResource(R.string.setup_device_owner),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .clickable { navViewModel.navigateTo("setup_device_owner") }
                            )
                        }
                        if (!advancedBlocking) {
                            Text(stringResource(R.string.protection_needs_accessibility))
                            Text(
                                stringResource(R.string.advanced_blocking),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .clickable { navViewModel.navigateTo("setup_device_owner") }
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                stringResource(R.string.protection_layers_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            )
        }

        item {
            LayerRow(
                icon = Icons.Outlined.ImageSearch,
                title = stringResource(R.string.visual_blocking),
                status = stringResource(R.string.always_on),
                onClick = { navViewModel.navigateTo("visual_blocking") }
            )
        }
        item {
            LayerRow(
                icon = Icons.Outlined.NoAdultContent,
                title = stringResource(R.string.blocked_words_feat),
                status = stringResource(R.string.always_on),
                onClick = { navViewModel.navigateTo("blocked_words") }
            )
        }
        item {
            LayerRow(
                icon = Icons.Outlined.Web,
                title = stringResource(R.string.browser_restrictions),
                status = stringResource(R.string.always_on),
                onClick = { navViewModel.navigateTo("browser_restrictions") }
            )
        }
        item {
            LayerRow(
                icon = Icons.Outlined.Block,
                title = stringResource(R.string.system_restrictions),
                status = stringResource(R.string.always_on),
                onClick = { navViewModel.navigateTo("system_restrictions") }
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun LayerRow(
    icon: ImageVector,
    title: String,
    status: String,
    onClick: (() -> Unit)?
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(status) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
