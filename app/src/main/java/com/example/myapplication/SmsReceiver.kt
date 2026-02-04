package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime

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

        // Network work must be off the main thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                postJson(endpoint, json.toString())
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Failed to POST SMS to server", e)
            }
        }
    }

    private fun postJson(url: String, jsonBody: String) {
        val conn = (URL(url).openConnection() as HttpURLConnection)
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            conn.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            if (code !in 200..299) {
                val err = try {
                    conn.errorStream?.readBytes()?.toString(Charsets.UTF_8)
                } catch (_: Exception) {
                    null
                }
                throw RuntimeException("Server returned HTTP $code: ${err ?: "(no body)"}")
            }
        } finally {
            conn.disconnect()
        }
    }
}
