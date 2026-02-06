package com.example.myapplication.ui

package com.example.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.myapplication.AppConfig
import com.example.myapplication.SmsForwardWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun StatusScreen(
    modifier: Modifier = Modifier,
    serverUrl: String,
    useHmacOnly: Boolean,
    manualTestWorkInfos: List<WorkInfo>,
    smsForwardWorkInfos: List<WorkInfo>,
    summarizeWorkState: (List<WorkInfo>) -> String,
    onRefresh: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var refreshNonce = remember { mutableIntStateOf(0) }

    val last = remember(refreshNonce.intValue) {
        AppConfig.getLastForwardStatus(ctx)
    }

    val healthState = remember { mutableStateOf<HealthResult?>(null) }
    val healthBusy = remember { mutableStateOf(false) }

    // Auto-refresh Last send card whenever WorkManager state changes.
    LaunchedEffect(
        manualTestWorkInfos,
        smsForwardWorkInfos
    ) {
        refreshNonce.intValue++
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Status", style = MaterialTheme.typography.titleLarge)

        Card {
            Column(Modifier.padding(12.dp)) {
                Text("Configuration", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("Server: $serverUrl")
                Text("HMAC-only: $useHmacOnly")
            }
        }

        Card {
            Column(Modifier.padding(12.dp)) {
                Text("WorkManager", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))

                Text("manual-test: ${summarizeWorkState(manualTestWorkInfos)}")
                val manualCounts = countStates(manualTestWorkInfos)
                if (manualTestWorkInfos.isNotEmpty()) {
                    Text(
                        "  RUNNING=${manualCounts.running} ENQUEUED=${manualCounts.enqueued} BLOCKED=${manualCounts.blocked} SUCCEEDED=${manualCounts.succeeded} FAILED=${manualCounts.failed} CANCELLED=${manualCounts.cancelled}"
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text("sms-forward: ${summarizeWorkState(smsForwardWorkInfos)} (total=${smsForwardWorkInfos.size})")
                val smsCounts = countStates(smsForwardWorkInfos)
                if (smsForwardWorkInfos.isNotEmpty()) {
                    Text(
                        "  RUNNING=${smsCounts.running} ENQUEUED=${smsCounts.enqueued} BLOCKED=${smsCounts.blocked} SUCCEEDED=${smsCounts.succeeded} FAILED=${smsCounts.failed} CANCELLED=${smsCounts.cancelled}"
                    )
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        WorkManager.getInstance(ctx).cancelAllWorkByTag(SmsForwardWorker.TAG_SMS_FORWARD)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel sms-forward work")
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = when {
                    !last.hasData -> MaterialTheme.colorScheme.surfaceVariant
                    last.ok -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("Last send", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))

                if (!last.hasData) {
                    Text("No sends yet.")
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (last.ok) "SUCCESS" else "FAILED",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.weight(1f))
                        Text(timeAgo(last.attemptAtEpochMs), style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(6.dp))
                    Text("HTTP: ${last.httpCode}")
                    Text("When: ${formatEpochMs(last.attemptAtEpochMs)}")
                    Text("Endpoint: ${last.endpoint ?: "(none)"}")
                    if (!last.errorBody.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("Error:")
                        Text(last.errorBody!!, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        refreshNonce.intValue++
                        onRefresh()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Refresh")
                }
            }
        }

        Card {
            Column(Modifier.padding(12.dp)) {
                Text("Server health", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))

                val h = healthState.value
                when (h) {
                    null -> Text("Not checked yet.")
                    is HealthResult.Ok -> {
                        Text("OK")
                        Text("Response: ${h.body}", style = MaterialTheme.typography.bodySmall)
                    }
                    is HealthResult.Err -> {
                        Text("ERROR")
                        Text(h.message, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            healthBusy.value = true
                            healthState.value = checkHealth(serverUrl)
                            healthBusy.value = false
                        }
                    },
                    enabled = !healthBusy.value,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (healthBusy.value) "Checking…" else "Check /health")
                }
            }
        }

        Text("Tip: you can also open $serverUrl/health in your phone browser", style = MaterialTheme.typography.bodySmall)
    }
}

private sealed class HealthResult {
    data class Ok(val body: String) : HealthResult()
    data class Err(val message: String) : HealthResult()
}

private suspend fun checkHealth(serverUrl: String): HealthResult = withContext(Dispatchers.IO) {
    val base = serverUrl.trim().trimEnd('/')
    val url = "$base/health"
    try {
        val conn = (URL(url).openConnection() as HttpURLConnection)
        conn.requestMethod = "GET"
        conn.connectTimeout = 6_000
        conn.readTimeout = 6_000
        val code = conn.responseCode
        val body = if (code in 200..299) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "(no error body)"
        }
        if (code in 200..299) HealthResult.Ok(body) else HealthResult.Err("HTTP $code: $body")
    } catch (e: Exception) {
        HealthResult.Err(e.toString())
    }
}

private data class WorkStateCounts(
    val running: Int,
    val enqueued: Int,
    val succeeded: Int,
    val failed: Int,
    val blocked: Int,
    val cancelled: Int,
)

private fun countStates(infos: List<WorkInfo>): WorkStateCounts {
    var running = 0
    var enqueued = 0
    var succeeded = 0
    var failed = 0
    var blocked = 0
    var cancelled = 0

    infos.forEach { wi ->
        when (wi.state) {
            WorkInfo.State.RUNNING -> running++
            WorkInfo.State.ENQUEUED -> enqueued++
            WorkInfo.State.SUCCEEDED -> succeeded++
            WorkInfo.State.FAILED -> failed++
            WorkInfo.State.BLOCKED -> blocked++
            WorkInfo.State.CANCELLED -> cancelled++
        }
    }

    return WorkStateCounts(
        running = running,
        enqueued = enqueued,
        succeeded = succeeded,
        failed = failed,
        blocked = blocked,
        cancelled = cancelled,
    )
}

private fun formatEpochMs(epochMs: Long): String {
    if (epochMs <= 0L) return "(unknown time)"
    return try {
        val inst = java.time.Instant.ofEpochMilli(epochMs)
        val zdt = java.time.ZonedDateTime.ofInstant(inst, java.time.ZoneId.systemDefault())
        zdt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    } catch (_: Exception) {
        epochMs.toString()
    }
}

private fun timeAgo(epochMs: Long): String {
    if (epochMs <= 0L) return ""
    val diffSec = ((System.currentTimeMillis() - epochMs) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 10 -> "just now"
        diffSec < 60 -> "${diffSec}s ago"
        diffSec < 3600 -> "${diffSec / 60}m ago"
        diffSec < 86400 -> "${diffSec / 3600}h ago"
        else -> "${diffSec / 86400}d ago"
    }
}
