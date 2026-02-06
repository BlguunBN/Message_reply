package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.myapplication.ui.AppScreen
import com.example.myapplication.ui.DebugScreen
import com.example.myapplication.ui.SettingsScreen
import com.example.myapplication.ui.StatusScreen
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
        AppRoot(modifier = modifier)
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
                Text("Sign up")
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

private fun summarizeWorkState(infos: List<WorkInfo>): String {
    if (infos.isEmpty()) return "(none)"
    val preferred = infos.firstOrNull { it.state == WorkInfo.State.RUNNING }
        ?: infos.firstOrNull { it.state == WorkInfo.State.ENQUEUED }
        ?: infos.last()
    return "${preferred.state} (attempts=${preferred.runAttemptCount})"
}

@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current

    var serverUrl by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var useHmacOnly by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        serverUrl = AppConfig.getServerBaseUrl(ctx)
        secret = AppConfig.getSecret(ctx)
        useHmacOnly = AppConfig.getUseHmacOnly(ctx)
    }

    val wm = remember { WorkManager.getInstance(ctx) }
    val manualTestWorkInfos by wm.getWorkInfosForUniqueWorkLiveData("manual-test")
        .observeAsState(initial = emptyList())

    val smsForwardWorkInfos by wm.getWorkInfosByTagLiveData(SmsForwardWorker.TAG_SMS_FORWARD)
        .observeAsState(initial = emptyList())

    val nav = rememberNavController()
    val tabs = listOf(AppScreen.Status, AppScreen.Settings, AppScreen.Debug)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                val current = nav.currentBackStackEntry?.destination?.route
                tabs.forEach { screen ->
                    NavigationBarItem(
                        selected = current == screen.route,
                        onClick = {
                            nav.navigate(screen.route) {
                                launchSingleTop = true
                                popUpTo(AppScreen.Status.route) { saveState = true }
                                restoreState = true
                            }
                        },
                        icon = { /* no icons for now */ },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { inner ->
        Box(Modifier.padding(inner)) {
            NavHost(navController = nav, startDestination = AppScreen.Status.route) {
                composable(AppScreen.Status.route) {
                    StatusScreen(
                        serverUrl = serverUrl,
                        useHmacOnly = useHmacOnly,
                        manualTestWorkInfos = manualTestWorkInfos,
                        smsForwardWorkInfos = smsForwardWorkInfos,
                        summarizeWorkState = ::summarizeWorkState,
                        onRefresh = { /* handled inside */ },
                    )
                }

                composable(AppScreen.Settings.route) {
                    SettingsScreen(
                        serverUrl = serverUrl,
                        secret = secret,
                        useHmacOnly = useHmacOnly,
                        onServerUrlChange = { serverUrl = it },
                        onSecretChange = { secret = it },
                        onUseHmacOnlyChange = { useHmacOnly = it },
                        onSaved = { /* TODO snackbar */ }
                    )
                }

                composable(AppScreen.Debug.route) {
                    DebugScreen(
                        serverUrl = serverUrl,
                        secret = secret,
                        useHmacOnly = useHmacOnly,
                        manualTestWorkInfos = manualTestWorkInfos,
                        smsForwardWorkInfos = smsForwardWorkInfos,
                        summarizeWorkState = ::summarizeWorkState,
                        onStatus = { /* TODO snackbar */ }
                    )
                }
            }
        }
    }
}
