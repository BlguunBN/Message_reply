package com.example.myapplication.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.myapplication.AppConfig
import com.example.myapplication.SmsForwardWorker
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Composable
fun DebugScreen(
    modifier: Modifier = Modifier,
    serverUrl: String,
    secret: String,
    useHmacOnly: Boolean,
    manualTestWorkInfos: List<WorkInfo>,
    smsForwardWorkInfos: List<WorkInfo>,
    summarizeWorkState: (List<WorkInfo>) -> String,
    onStatus: (String) -> Unit,
) {
    val ctx = LocalContext.current

    fun copyDebugInfoToClipboard() {
        val last = AppConfig.getLastForwardStatus(ctx)
        val manualSummary = summarizeWorkState(manualTestWorkInfos)
        val smsSummary = summarizeWorkState(smsForwardWorkInfos)

        val sb = StringBuilder()
        sb.appendLine("SMS Bridge Debug Info")
        sb.appendLine("serverUrl=$serverUrl")
        sb.appendLine("useHmacOnly=$useHmacOnly")
        sb.appendLine("work.manualTest=$manualSummary")
        sb.appendLine("work.smsForward=$smsSummary count=${smsForwardWorkInfos.size}")
        sb.appendLine("last.ok=${last.ok}")
        sb.appendLine("last.http=${last.httpCode}")
        sb.appendLine("last.when=${last.attemptAtEpochMs}")
        sb.appendLine("last.endpoint=${last.endpoint ?: "(none)"}")
        if (!last.errorBody.isNullOrBlank()) sb.appendLine("last.error=${last.errorBody}")

        val clip = ClipData.newPlainText("sms-bridge-debug", sb.toString())
        val clipboard = ctx.getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(clip)
    }

    Column(modifier = modifier.padding(16.dp)) {
        Text("Debug")
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                val s = secret.trim()
                val token = AppConfig.getAuthToken(ctx)

                if (token.isBlank() && s.isBlank()) {
                    onStatus("❌ Not logged in and secret is blank. Log in or set secret.")
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
                    .setInputData(SmsForwardWorker.inputData(endpoint, json, s, token))
                    .setBackoffCriteria(
                        SmsForwardWorker.backoffPolicy,
                        SmsForwardWorker.backoffDelay.toMillis(),
                        TimeUnit.MILLISECONDS
                    )
                    .build()

                WorkManager.getInstance(ctx)
                    .enqueueUniqueWork("manual-test", ExistingWorkPolicy.REPLACE, req)

                onStatus("✅ Enqueued test send (manual-test).")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send test message")
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                copyDebugInfoToClipboard()
                onStatus("📋 Copied debug info to clipboard")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Copy debug info")
        }

        Spacer(Modifier.height(12.dp))
        Text("WorkManager:")
        Text("- manual-test: ${summarizeWorkState(manualTestWorkInfos)}")
        Text("- sms-forward: ${summarizeWorkState(smsForwardWorkInfos)} (count=${smsForwardWorkInfos.size})")

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                WorkManager.getInstance(ctx).cancelAllWorkByTag(SmsForwardWorker.TAG_SMS_FORWARD)
                onStatus("🧹 Canceled sms-forward work")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel sms-forward work")
        }
    }
}
