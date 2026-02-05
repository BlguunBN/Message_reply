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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                    AuthGate(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AuthGate(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var token by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        token = AppConfig.getAuthToken(ctx)
    }

    if (token.isBlank()) {
        AuthScreen(modifier = modifier, onAuthed = { newToken ->
            AppConfig.setAuthToken(ctx, newToken)
            token = newToken
        })
    } else {
        SettingsScreenAuthed(modifier = modifier, onLogout = {
            AppConfig.clearAuth(ctx)
            token = ""
        })
    }
}

@Composable
fun AuthScreen(modifier: Modifier = Modifier, onAuthed: (String) -> Unit) {
    val ctx = LocalContext.current

    var serverUrl by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("login") } // login|signup

    // login
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // signup
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password2 by remember { mutableStateOf("") }

    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        serverUrl = AppConfig.getServerBaseUrl(ctx)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("SMS → Telegram Bridge")
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("Server base URL") },
            placeholder = { Text("http://10.0.2.2:3000") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        RowButtons(
            left = "Login",
            right = "Sign up",
            active = mode,
            onLeft = { mode = "login"; status = "" },
            onRight = { mode = "signup"; status = "" },
        )

        Spacer(Modifier.height(12.dp))

        if (mode == "login") {
            OutlinedTextField(
                value = identifier,
                onValueChange = { identifier = it },
                label = { Text("Username or Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            Button(
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    AppConfig.setServerBaseUrl(ctx, serverUrl)
                    busy = true
                    status = "Logging in..."
                    scope.launch {
                        val res = withContext(Dispatchers.IO) {
                            AuthApi.login(serverUrl.trim().trimEnd('/'), identifier.trim(), password)
                        }
                        busy = false
                        if (res.ok && !res.token.isNullOrBlank()) {
                            status = "✅ Logged in"
                            onAuthed(res.token!!)
                        } else {
                            status = "❌ ${res.error ?: "Login failed"}"
                        }
                    }
                }
            ) {
                Text("Login")
            }
        } else {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password2,
                onValueChange = { password2 = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            Button(
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    AppConfig.setServerBaseUrl(ctx, serverUrl)
                    busy = true
                    status = "Signing up..."
                    scope.launch {
                        val res = withContext(Dispatchers.IO) {
                            AuthApi.signup(serverUrl.trim().trimEnd('/'), username.trim(), email.trim(), password2)
                        }
                        busy = false
                        if (res.ok && !res.token.isNullOrBlank()) {
                            status = "✅ Signed up"
                            onAuthed(res.token!!)
                        } else {
                            status = "❌ ${res.error ?: "Signup failed"}"
                        }
                    }
                }
            ) {
                Text("Create account")
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(status)

        Spacer(Modifier.height(16.dp))
        Text("After login, SMS forwarding uses Bearer token auth.")
    }
}

@Composable
fun RowButtons(
    left: String,
    right: String,
    active: String,
    onLeft: () -> Unit,
    onRight: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onLeft,
            modifier = Modifier.weight(1f),
            enabled = active != "login"
        ) { Text(left) }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onRight,
            modifier = Modifier.weight(1f),
            enabled = active != "signup"
        ) { Text(right) }
    }
}

@Composable
fun SettingsScreenAuthed(modifier: Modifier = Modifier, onLogout: () -> Unit) {
    val ctx = LocalContext.current

    var serverUrl by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        serverUrl = AppConfig.getServerBaseUrl(ctx)
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
        Text("Settings")
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
        Text("2) Configure server")
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("Server base URL") },
            placeholder = { Text("http://10.0.2.2:3000") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                AppConfig.setServerBaseUrl(ctx, serverUrl)
                status = "✅ Saved"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }

        Spacer(Modifier.height(12.dp))
        Text(status)

        Spacer(Modifier.height(16.dp))
        Text("Emulator tip: open http://10.0.2.2:3000/health in the emulator browser")
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAuth() {
    MyApplicationTheme {
        AuthScreen(onAuthed = {})
    }
}
