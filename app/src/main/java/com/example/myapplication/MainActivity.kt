package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppRoot()
            }
        }
    }
}

private fun summarizeWorkState(infos: List<WorkInfo>): String {
    if (infos.isEmpty()) return "(none)"
    // Prefer RUNNING, else ENQUEUED, else last item.
    val preferred = infos.firstOrNull { it.state == WorkInfo.State.RUNNING }
        ?: infos.firstOrNull { it.state == WorkInfo.State.ENQUEUED }
        ?: infos.last()
    return "${preferred.state} (attempts=${preferred.runAttemptCount})"
}

@androidx.compose.runtime.Composable
fun AppRoot() {
    val ctx = LocalContext.current

    var serverUrl by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var useHmacOnly by remember { mutableStateOf(false) }
    var statusLine by remember { mutableStateOf("") }

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
        modifier = Modifier.fillMaxSize(),
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
                        onRefresh = {
                            statusLine = "🔄 Refreshed"
                        }
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
                        onSaved = { statusLine = it }
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
                        onStatus = { statusLine = it }
                    )
                }
            }

            // TODO: show statusLine via SnackbarHost
        }
    }
}
