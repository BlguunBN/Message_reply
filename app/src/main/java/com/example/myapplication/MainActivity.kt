package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.app.AppScaffold
import com.example.myapplication.ui.app.OnboardingScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Root(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun Root(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val cfg = LocalConfiguration.current

    val isWide = cfg.screenWidthDp >= 600

    var token by remember { mutableStateOf("") }
    var onboardingDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        token = AppConfig.getAuthToken(ctx)
    }

    if (token.isBlank()) {
        if (!onboardingDone) {
            OnboardingScreen(
                modifier = modifier,
                onContinue = { onboardingDone = true }
            )
        } else {
            AuthScreen(
                modifier = modifier,
                onAuthed = { newToken ->
                    AppConfig.setAuthToken(ctx, newToken)
                    token = newToken
                }
            )
        }
    } else {
        AppScaffold(
            modifier = modifier,
            isWide = isWide,
            onLogout = {
                AppConfig.clearAuth(ctx)
                token = ""
                onboardingDone = false
            }
        )
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
        Text("Message Reply")
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

            val pwBytes = password2.encodeToByteArray().size
            val pwTooLong = pwBytes > 72

            OutlinedTextField(
                value = password2,
                onValueChange = { password2 = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            if (pwTooLong) {
                Text(
                    text = "Password is ${pwBytes} bytes. If server uses bcrypt, max is 72 bytes.",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "Password length: ${pwBytes}/72 bytes (bcrypt limit)",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))
            Button(
                enabled = !busy && !pwTooLong,
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
                Text(if (pwTooLong) "Password too long" else "Sign up")
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(status)
    }
}

@Composable
private fun RowButtons(
    left: String,
    right: String,
    active: String,
    onLeft: () -> Unit,
    onRight: () -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        Button(
            onClick = onLeft,
            modifier = Modifier.weight(1f),
            enabled = active != "login"
        ) { Text(left) }
        Spacer(Modifier.weight(0.1f))
        Button(
            onClick = onRight,
            modifier = Modifier.weight(1f),
            enabled = active != "signup"
        ) { Text(right) }
    }
}
