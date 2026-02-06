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
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.myapplication.ui.theme.MyApplicationTheme
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

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
    var useHmacOnly by remember { mutableStateOf(false) }

    var status by remember { mutableStateOf("") }
    var lastForwardStatus by remember { mutableStateOf(AppConfig.getLastForwardStatus(ctx)) }

    fun refreshLastStatus() {
        lastForwardStatus = AppConfig.getLastForwardStatus(ctx)
    }

    val wm = remember { WorkManager.getInstance(ctx) }
    val manualTestWorkInfos by wm.getWorkInfosForUniqueWorkLiveData("manual-test")
        .observeAsState(initial = emptyList())

    val smsForwardWorkInfos by wm.getWorkInfosByTagLiveData(SmsForwardWorker.TAG_SMS_FORWARD)
        .observeAsState(initial = emptyList())

    fun summarizeWorkState(infos: List<WorkInfo>): String {
        if (infos.isEmpty()) return "(none)"
        // Prefer RUNNING, else ENQUEUED, else last item.
        val preferred = infos.firstOrNull { it.state == WorkInfo.State.RUNNING }
            ?: infos.firstOrNull { it.state == WorkInfo.State.ENQUEUED }
            ?: infos.last()
        return "${preferred.state} (attempts=${preferred.runAttemptCount})"
    }

    LaunchedEffect(Unit) {
        serverUrl = AppConfig.getServerBaseUrl(ctx)
        secret = AppConfig.getSecret(ctx)
        useHmacOnly = AppConfig.getUseHmacOnly(ctx)
        refreshLastStatus()
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Use HMAC only",
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = useHmacOnly,
                onCheckedChange = { useHmacOnly = it }
            )
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                AppConfig.setServerBaseUrl(ctx, serverUrl)
                AppConfig.setSecret(ctx, secret)
                AppConfig.setUseHmacOnly(ctx, useHmacOnly)

                status = "✅ Saved settings"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save settings")
        }

        Spacer(Modifier.height(16.dp))
        Text("3) Debug tools")
        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                // Enqueue a fake SMS forward so we can debug networking/auth quickly.
                val s = secret.trim()
                if (s.isBlank()) {
                    status = "❌ Secret is blank. Set secret first."
                    return@Button
                }

                val base = serverUrl.trim().trimEnd('/')
                val endpoint = "$base/sms/incoming"

                val receivedAt = try {
                    OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                } catch (_: Exception) {
                    null
                }

                val json = JSONObject().apply {
                    if (!useHmacOnly) put("secret", s)
                    put("from", "TEST")
                    put("body", "E2E test from app button @ ${System.currentTimeMillis()}")
                    if (receivedAt != null) put("receivedAt", receivedAt)
                }.toString()

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val req = OneTimeWorkRequestBuilder<SmsForwardWorker>()
                    .addTag(SmsForwardWorker.TAG_SMS_FORWARD)
                    .setConstraints(constraints)
                    .setInputData(SmsForwardWorker.inputData(endpoint, json, s))
                    .setBackoffCriteria(
                        SmsForwardWorker.backoffPolicy,
                        SmsForwardWorker.backoffDelay.toMillis(),
                        TimeUnit.MILLISECONDS
                    )
                    .build()

                WorkManager.getInstance(ctx)
                    .enqueueUniqueWork("manual-test", ExistingWorkPolicy.REPLACE, req)

                status = "✅ Enqueued test send (manual-test). Check status below."
            },
            modifier = Modifier.fillMaxWidth()
        ) { 
            Text("Send test message")
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                refreshLastStatus()
                status = "🔄 Refreshed status"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Refresh status")
        }

        Spacer(Modifier.height(12.dp))

        Text("WorkManager status:")
        Text("- manual-test: ${summarizeWorkState(manualTestWorkInfos)}")
        Text("- sms-forward jobs: ${summarizeWorkState(smsForwardWorkInfos)} (count=${smsForwardWorkInfos.size})")

        Spacer(Modifier.height(12.dp))

        val last = lastForwardStatus
        if (!last.hasData) {
            Text("Last forward: (none yet)")
        } else {
            val whenStr = if (last.attemptAtEpochMs > 0L) {
                try {
                    val inst = java.time.Instant.ofEpochMilli(last.attemptAtEpochMs)
                    val zdt = java.time.ZonedDateTime.ofInstant(inst, java.time.ZoneId.systemDefault())
                    zdt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                } catch (_: Exception) {
                    last.attemptAtEpochMs.toString()
                }
            } else "(unknown time)"

            Text("Last HTTP result:")
            Text("- ok: ${last.ok}")
            Text("- http: ${last.httpCode}")
            Text("- when: $whenStr")
            Text("- endpoint: ${last.endpoint ?: "(none)"}")
            if (!last.errorBody.isNullOrBlank()) {
                Text("- error: ${last.errorBody}")
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(status)

        Spacer(Modifier.height(16.dp))
        Text("Tip: from Android browser, open $serverUrl/health")
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSettings() {
    MyApplicationTheme {
        SettingsScreen()
    }
}
