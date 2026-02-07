@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.myapplication.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.myapplication.AppConfig
import com.example.myapplication.ui.components.AppCard
import com.example.myapplication.ui.components.ConfirmDialog
import com.example.myapplication.ui.components.PrimaryButton
import com.example.myapplication.ui.components.SecondaryButton
import com.example.myapplication.ui.components.StatusChip
import com.example.myapplication.ui.components.StatusChipTone
import com.example.myapplication.ui.model.FakeData
import com.example.myapplication.ui.model.TokenStatus
import com.example.myapplication.ui.theme.UiDimens
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onOpenTokens: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbars = remember { SnackbarHostState() }

    val loggedIn = AppConfig.getAuthToken(context).isNotBlank()
    var notificationsEnabled by remember { mutableStateOf(true) }
    var syncOnCellular by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(text = "Settings") }) },
        snackbarHost = { SnackbarHost(hostState = snackbars) },
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = UiDimens.screenPadding),
            contentPadding = PaddingValues(vertical = UiDimens.d16),
            verticalArrangement = Arrangement.spacedBy(UiDimens.sectionSpacing),
        ) {
            item {
                AppCard {
                    Text(text = "Account", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (loggedIn) "Signed in" else "Not signed in",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SecondaryButton(
                        text = "API Tokens",
                        onClick = onOpenTokens,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PrimaryButton(
                        text = "Logout",
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = loggedIn,
                    )
                }
            }

            item {
                AppCard {
                    Text(text = "Notifications", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    SettingToggleRow(
                        title = "Push notifications",
                        subtitle = "Alerts for failed or delayed sends",
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it },
                    )
                    SettingToggleRow(
                        title = "Sync on cellular",
                        subtitle = "Allow background sync when not on Wi-Fi",
                        checked = syncOnCellular,
                        onCheckedChange = { syncOnCellular = it },
                    )
                }
            }

            item {
                AppCard {
                    Text(text = "Integrations", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    SettingLinkRow(
                        title = "Webhook URL",
                        value = AppConfig.getServerBaseUrl(context),
                        onClick = {
                            scope.launch {
                                snackbars.showSnackbar("Webhook URL copied to clipboard is not implemented yet")
                            }
                        },
                    )
                    SettingLinkRow(
                        title = "Auth method",
                        value = "Bearer token",
                        onClick = {},
                    )
                }
            }
        }
    }
}

@Composable
fun TokensScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var revokeDialogOpen by remember { mutableStateOf(false) }
    var selectedTokenId by remember { mutableStateOf<String?>(null) }
    var revokedIds by remember { mutableStateOf(setOf<String>()) }

    ConfirmDialog(
        open = revokeDialogOpen,
        title = "Revoke API token?",
        body = "This action is irreversible. Any client using this token will lose access immediately.",
        confirmText = "Revoke",
        isDestructive = true,
        onConfirm = {
            val id = selectedTokenId
            if (id != null) {
                revokedIds = revokedIds + id
                scope.launch { snackbarHostState.showSnackbar("Token revoked") }
            }
            revokeDialogOpen = false
            selectedTokenId = null
        },
        onDismiss = {
            revokeDialogOpen = false
            selectedTokenId = null
        },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "API Tokens") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = UiDimens.screenPadding),
            contentPadding = PaddingValues(vertical = UiDimens.d16),
            verticalArrangement = Arrangement.spacedBy(UiDimens.listItemSpacing),
        ) {
            items(FakeData.tokens, key = { it.id }) { token ->
                val effectiveStatus = if (token.id in revokedIds) TokenStatus.REVOKED else token.status

                AppCard {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(UiDimens.d4)) {
                            Text(text = token.label, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Created: ${token.createdAt}",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Last used: ${token.lastUsedAt ?: "Never"}",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Expires: ${token.expiresAt ?: "No expiry"}",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        StatusChip(
                            label = if (effectiveStatus == TokenStatus.ACTIVE) "Active" else "Revoked",
                            tone = if (effectiveStatus == TokenStatus.ACTIVE) {
                                StatusChipTone.Success
                            } else {
                                StatusChipTone.Error
                            },
                        )
                    }

                    PrimaryButton(
                        text = "Revoke",
                        onClick = {
                            selectedTokenId = token.id
                            revokeDialogOpen = true
                        },
                        enabled = effectiveStatus == TokenStatus.ACTIVE,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Revoking a token cannot be undone.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(UiDimens.d4),
        ) {
            Text(text = title, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingLinkRow(
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = UiDimens.d8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(UiDimens.d4),
        ) {
            Text(text = title, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
            Text(
                text = value,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
    }
}
