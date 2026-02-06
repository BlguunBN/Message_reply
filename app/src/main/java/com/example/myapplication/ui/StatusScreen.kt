package com.example.myapplication.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.myapplication.AppConfig
import com.example.myapplication.SmsForwardWorker

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
    val last = AppConfig.getLastForwardStatus(ctx)

    Column(modifier = modifier.padding(16.dp)) {
        Text("Status")
        Spacer(Modifier.height(12.dp))

        Text("Configuration:")
        Text("- server: $serverUrl")
        Text("- HMAC-only: $useHmacOnly")

        Spacer(Modifier.height(12.dp))
        Text("WorkManager:")
        Text("- manual-test: ${summarizeWorkState(manualTestWorkInfos)}")
        Text("- sms-forward: ${summarizeWorkState(smsForwardWorkInfos)} (count=${smsForwardWorkInfos.size})")

        Spacer(Modifier.height(12.dp))
        Text("Last HTTP result:")
        if (!last.hasData) {
            Text("- (none yet)")
        } else {
            Text("- ok: ${last.ok}")
            Text("- http: ${last.httpCode}")
            Text("- when: ${formatEpochMs(last.attemptAtEpochMs)}")
            Text("- endpoint: ${last.endpoint ?: "(none)"}")
            if (!last.errorBody.isNullOrBlank()) {
                Text("- error: ${last.errorBody}")
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
            Text("Refresh")
        }

        Spacer(Modifier.height(12.dp))
        Text("Tip: open $serverUrl/health in your phone browser")
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                // Convenience: cancel stuck/retrying work.
                WorkManager.getInstance(ctx).cancelAllWorkByTag(SmsForwardWorker.TAG_SMS_FORWARD)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel sms-forward work")
        }
    }
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
