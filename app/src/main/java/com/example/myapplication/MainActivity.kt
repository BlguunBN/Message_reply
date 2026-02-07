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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.example.myapplication.ui.app.AppScaffold
import com.example.myapplication.ui.app.OnboardingScreen
import com.example.myapplication.ui.components.AppCard
import com.example.myapplication.ui.components.PrimaryButton
import com.example.myapplication.ui.components.SecondaryButton
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.UiDimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isWide = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Root(isWide = isWide)
                }
            }
        }
    }
}

@Composable
fun Root(
    isWide: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var token by remember { mutableStateOf("") }
    var onboardingDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        token = AppConfig.getAuthToken(context)
    }

    if (token.isBlank()) {
        if (!onboardingDone) {
            OnboardingScreen(
                modifier = modifier,
                onContinue = { onboardingDone = true },
            )
        } else {
            AuthScreen(
                modifier = modifier,
                onAuthed = { newToken ->
                    AppConfig.setAuthToken(context, newToken)
                    token = newToken
                },
            )
        }
    } else {
        AppScaffold(
            modifier = modifier,
            isWide = isWide,
            onLogout = {
                AppConfig.clearAuth(context)
                token = ""
                onboardingDone = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    onAuthed: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var serverUrl by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("login") }

    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }

    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        serverUrl = AppConfig.getServerBaseUrl(context)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(text = "Sign in") }) },
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(UiDimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(UiDimens.sectionSpacing),
        ) {
            AppCard {
                Text(text = "Connect to your Message Reply server", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text(text = "Server base URL") },
                    placeholder = { Text(text = "http://10.0.2.2:3000") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(UiDimens.d8)) {
                SecondaryButton(
                    text = "Login",
                    onClick = {
                        mode = "login"
                        status = null
                    },
                    modifier = Modifier.weight(1f),
                    enabled = mode != "login",
                )
                SecondaryButton(
                    text = "Sign up",
                    onClick = {
                        mode = "signup"
                        status = null
                    },
                    modifier = Modifier.weight(1f),
                    enabled = mode != "signup",
                )
            }

            AppCard {
                if (mode == "login") {
                    OutlinedTextField(
                        value = identifier,
                        onValueChange = { identifier = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Username or Email") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                    )
                    PrimaryButton(
                        text = if (busy) "Logging in..." else "Login",
                        onClick = {
                            AppConfig.setServerBaseUrl(context, serverUrl)
                            busy = true
                            status = "Logging in..."
                            scope.launch {
                                val response = withContext(Dispatchers.IO) {
                                    AuthApi.login(
                                        serverUrl.trim().trimEnd('/'),
                                        identifier.trim(),
                                        password,
                                    )
                                }
                                busy = false
                                if (response.ok && !response.token.isNullOrBlank()) {
                                    status = "Logged in"
                                    onAuthed(response.token!!)
                                } else {
                                    status = response.error ?: "Login failed"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy && serverUrl.isNotBlank() && identifier.isNotBlank() && password.isNotBlank(),
                    )
                } else {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Username") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Email") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = signupPassword,
                        onValueChange = { signupPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                    )

                    val passwordBytes = signupPassword.encodeToByteArray().size
                    val passwordTooLong = passwordBytes > 72

                    Text(
                        text = if (passwordTooLong) {
                            "Password is $passwordBytes bytes. bcrypt max is 72 bytes."
                        } else {
                            "Password length: $passwordBytes/72 bytes"
                        },
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = if (passwordTooLong) {
                            androidx.compose.material3.MaterialTheme.colorScheme.error
                        } else {
                            androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )

                    PrimaryButton(
                        text = if (busy) "Creating account..." else "Sign up",
                        onClick = {
                            AppConfig.setServerBaseUrl(context, serverUrl)
                            busy = true
                            status = "Creating account..."
                            scope.launch {
                                val response = withContext(Dispatchers.IO) {
                                    AuthApi.signup(
                                        serverUrl.trim().trimEnd('/'),
                                        username.trim(),
                                        email.trim(),
                                        signupPassword,
                                    )
                                }
                                busy = false
                                if (response.ok && !response.token.isNullOrBlank()) {
                                    status = "Account created"
                                    onAuthed(response.token!!)
                                } else {
                                    status = response.error ?: "Sign up failed"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy && !passwordTooLong && serverUrl.isNotBlank() && username.isNotBlank() && email.isNotBlank(),
                    )
                }
            }

            status?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(UiDimens.d8))
                    }
                    Text(
                        text = it,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
