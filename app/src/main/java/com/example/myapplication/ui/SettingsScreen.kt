package com.example.myapplication.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.myapplication.AppConfig

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    serverUrl: String,
    secret: String,
    useHmacOnly: Boolean,
    isLoggedIn: Boolean,
    onServerUrlChange: (String) -> Unit,
    onSecretChange: (String) -> Unit,
    onUseHmacOnlyChange: (Boolean) -> Unit,
    onSaved: (String) -> Unit,
    onLogout: () -> Unit,
) {
    val ctx = LocalContext.current

    var permissionStatus by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            permissionStatus = if (granted) "✅ SMS permission granted" else "❌ SMS permission denied"
        }
    )

    val hasSmsPermission = ContextCompat.checkSelfPermission(
        ctx,
        Manifest.permission.RECEIVE_SMS
    ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(Unit) {
        if (hasSmsPermission) permissionStatus = "✅ SMS permission granted"
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("Settings")
        Spacer(Modifier.height(12.dp))

        Text("Permissions")
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { permissionLauncher.launch(Manifest.permission.RECEIVE_SMS) },
            enabled = !hasSmsPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (hasSmsPermission) "Permission already granted" else "Grant RECEIVE_SMS")
        }
        Spacer(Modifier.height(8.dp))
        Text(permissionStatus)

        Spacer(Modifier.height(16.dp))
        Text("Account")
        Spacer(Modifier.height(8.dp))
        Text(if (isLoggedIn) "Logged in" else "Not logged in")
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onLogout,
            enabled = isLoggedIn,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }

        Spacer(Modifier.height(16.dp))
        Text("Server")
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = serverUrl,
            onValueChange = onServerUrlChange,
            label = { Text("Server base URL") },
            placeholder = { Text("http://10.0.2.2:3000") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = secret,
            onValueChange = onSecretChange,
            label = { Text("Shared secret") },
            placeholder = { Text("Same as SMS_BRIDGE_SECRET") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Use HMAC only", modifier = Modifier.weight(1f))
            Switch(checked = useHmacOnly, onCheckedChange = onUseHmacOnlyChange)
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                AppConfig.setServerBaseUrl(ctx, serverUrl)
                AppConfig.setSecret(ctx, secret)
                AppConfig.setUseHmacOnly(ctx, useHmacOnly)
                onSaved("✅ Saved settings")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }

        Spacer(Modifier.height(12.dp))
        Text("Tip: if HMAC-only is enabled, your server must accept HMAC auth.")
    }
}
