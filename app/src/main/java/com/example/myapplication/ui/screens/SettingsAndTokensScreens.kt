package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.AppConfig
import com.example.myapplication.ui.components.ConfirmRevokeDialog
import com.example.myapplication.ui.model.FakeData
import com.example.myapplication.ui.model.TokenStatus

@Composable
fun SettingsScreen(
    onOpenTokens: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val loggedIn = AppConfig.getAuthToken(ctx).isNotBlank()

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { inner ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Account", style = MaterialTheme.typography.titleMedium)
                    Text(if (loggedIn) "Signed in" else "Not signed in", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row {
                        Button(onClick = onOpenTokens) { Text("API tokens") }
                        Spacer(Modifier.width(12.dp))
                        Button(enabled = loggedIn, onClick = onLogout) { Text("Logout") }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Server", style = MaterialTheme.typography.titleMedium)
                    Text(AppConfig.getServerBaseUrl(ctx), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text(
                "This UI is currently using fake data for Inbox/Numbers/Automations.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TokensScreen(modifier: Modifier = Modifier) {
    var revokeOpen by remember { mutableStateOf(false) }
    var selectedToken by remember { mutableStateOf<String?>(null) }

    ConfirmRevokeDialog(
        open = revokeOpen,
        onConfirm = {
            revokeOpen = false
            selectedToken = null
        },
        onDismiss = {
            revokeOpen = false
        }
    )

    Scaffold(topBar = { TopAppBar(title = { Text("API Tokens") }) }) { inner ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FakeData.tokens.forEach { t ->
                val statusText = if (t.status == TokenStatus.ACTIVE) "Active" else "Revoked"
                val color = if (t.status == TokenStatus.ACTIVE) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(t.label, style = MaterialTheme.typography.titleMedium)
                                Text("Created: ${t.createdAt}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (t.lastUsedAt != null) {
                                    Text("Last used: ${t.lastUsedAt}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(statusText, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                        Button(
                            enabled = t.status == TokenStatus.ACTIVE,
                            onClick = {
                                selectedToken = t.id
                                revokeOpen = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Revoke")
                        }
                    }
                }
            }
        }
    }
}
