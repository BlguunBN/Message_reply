package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SettingsScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current

    var serverUrl by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        serverUrl = AppConfig.getServerBaseUrl(ctx)
        secret = AppConfig.getSecret(ctx)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            status = if (granted) "✅ SMS permission granted" else "❌ SMS permission denied"
        }
    )

    val hasSmsPermission = ContextCompat.checkSelfPermission(
        ctx,
        Manifest.permission.RECEIVE_SMS
    ) == PackageManager.PERMISSION_GRANTED

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("SMS → Telegram Bridge")
        Spacer(Modifier.height(12.dp))

        Text("1) Grant SMS permission")
        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { permissionLauncher.launch(Manifest.permission.RECEIVE_SMS) },
            enabled = !hasSmsPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (hasSmsPermission) "Permission already granted" else "Grant RECEIVE_SMS")
        }

        Spacer(Modifier.height(16.dp))
        Text("2) Configure server + secret")
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("Server base URL") },
            placeholder = { Text("http://10.0.2.2:3000") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = secret,
            onValueChange = { secret = it },
            label = { Text("Shared secret") },
            placeholder = { Text("Same as SMS_BRIDGE_SECRET") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                AppConfig.setServerBaseUrl(ctx, serverUrl)
                AppConfig.setSecret(ctx, secret)
                status = "✅ Saved. Now send a test SMS in Emulator → Extended controls → Phone → SMS"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }

        Spacer(Modifier.height(12.dp))
        Text(status)

        Spacer(Modifier.height(16.dp))
        Text("Emulator tip: open http://10.0.2.2:3000/health in the emulator browser")
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSettings() {
    MyApplicationTheme {
        SettingsScreen()
    }
}
