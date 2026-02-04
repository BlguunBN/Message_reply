package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.json.JSONObject
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION != intent.action) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val from = messages.first().originatingAddress ?: "unknown"
        val body = messages.joinToString(separator = "") { it.messageBody ?: "" }

        val receivedAt = try {
            OffsetDateTime.now().toString()
        } catch (_: Exception) {
            null
        }

        val secret = AppConfig.getSecret(context)
        if (secret.isBlank()) {
            Log.w("SmsReceiver", "Secret not set; ignoring incoming SMS")
            return
        }

        val baseUrl = AppConfig.getServerBaseUrl(context)
        val endpoint = "$baseUrl/sms/incoming"

        val json = JSONObject().apply {
            put("secret", secret)
            put("from", from)
            put("body", body)
            if (receivedAt != null) put("receivedAt", receivedAt)
        }

        val jsonStr = json.toString()

        // Queue + retry via WorkManager.
        // Use a fingerprint so we don't enqueue duplicates (multipart/retries).
        val workName = "sms-forward-" + sha256("$from\n$body\n${receivedAt ?: ""}")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val req = OneTimeWorkRequestBuilder<SmsForwardWorker>()
            .setConstraints(constraints)
            .setInputData(SmsForwardWorker.inputData(endpoint, jsonStr))
            .setBackoffCriteria(
                SmsForwardWorker.backoffPolicy,
                SmsForwardWorker.backoffDelay.toMillis(),
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName, ExistingWorkPolicy.KEEP, req)

        Log.i("SmsReceiver", "Enqueued SMS forward work: $workName")
    }

    private fun sha256(s: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(s.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
