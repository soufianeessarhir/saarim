package com.sarim.app.screens.setup

import android.content.Context
import android.os.UserManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarim.app.AppPrefs
import com.sarim.app.R
import com.sarim.app.data.DeviceAdmin
import com.sarim.app.data.ProtectionDefaults
import com.sarim.app.viewmodels.DeviceOwnerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The step between owner privileges and the commitment: the user sees what is already locked on,
 * picks the filtering resolver (and can add the factory-reset block), and confirms the setup.
 *
 * It has to come before the commitment because the commitment is the point where these choices
 * stop being changeable. Only after this confirmation does the app offer the duration picker.
 */
@Composable
fun SetupProtection(
    paddingValues: PaddingValues,
    onConfirmed: () -> Unit,
    viewModel: DeviceOwnerViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedHost by remember {
        mutableStateOf(viewModel.getPrivateDNS() ?: ProtectionDefaults.FILTERING_DNS_HOST)
    }
    var blockFactoryReset by remember { mutableStateOf(isFactoryResetBlocked()) }
    var acknowledged by remember { mutableStateOf(false) }
    var applying by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Security,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))

        Text(
            stringResource(R.string.protection_setup_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            stringResource(R.string.protection_setup_intro),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.protection_setup_layers_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                LayerCheck(stringResource(R.string.visual_blocking))
                LayerCheck(stringResource(R.string.blocked_words_feat))
                LayerCheck(stringResource(R.string.browser_restrictions))
                LayerCheck(stringResource(R.string.dns_settings))
                LayerCheck(stringResource(R.string.block_tor))
                LayerCheck(stringResource(R.string.block_torrents))
                LayerCheck(stringResource(R.string.system_restrictions))
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.protection_setup_dns_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.protection_setup_dns_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                ProtectionDefaults.FILTERING_DNS_HOSTS.forEach { host ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedHost = host }
                            .padding(vertical = 6.dp)
                    ) {
                        RadioButton(
                            selected = selectedHost == host,
                            onClick = { selectedHost = host }
                        )
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(
                                ProtectionDefaults.FILTERING_DNS_LABELS[host] ?: host,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(host, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { blockFactoryReset = !blockFactoryReset }
                    .padding(16.dp)
            ) {
                Checkbox(
                    checked = blockFactoryReset,
                    onCheckedChange = { blockFactoryReset = it }
                )
                Column(Modifier.padding(start = 8.dp)) {
                    Text(
                        stringResource(R.string.protection_setup_factory_reset),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        stringResource(R.string.protection_setup_factory_reset_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { acknowledged = !acknowledged }
                .padding(vertical = 8.dp)
        ) {
            Checkbox(checked = acknowledged, onCheckedChange = { acknowledged = it })
            Text(
                stringResource(R.string.protection_setup_confirm),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    applying = true
                    withContext(Dispatchers.IO) {
                        if (viewModel.getPrivateDNS() != selectedHost) {
                            runCatching { viewModel.setPrivateDNS(selectedHost) }
                        }
                        applyFactoryResetRestriction(blockFactoryReset)
                    }
                    applying = false
                    AppPrefs.markProtectionSetupConfirmed(context)
                    onConfirmed()
                }
            },
            enabled = acknowledged && !applying,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (applying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    stringResource(R.string.protection_setup_continue),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LayerCheck(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

private fun isFactoryResetBlocked(): Boolean = runCatching {
    DeviceAdmin.policyManager.getUserRestrictions(DeviceAdmin.component)
        .getBoolean(UserManager.DISALLOW_FACTORY_RESET)
}.getOrDefault(false)

private fun applyFactoryResetRestriction(enabled: Boolean) {
    runCatching {
        val dpm = DeviceAdmin.policyManager
        val admin = DeviceAdmin.component
        if (!dpm.isAdminActive(admin)) return
        if (enabled) {
            dpm.addUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)
        } else {
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)
        }
    }
}
