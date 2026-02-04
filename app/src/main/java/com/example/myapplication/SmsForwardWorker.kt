package com.example.myapplication

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SmsForwardWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val endpoint = inputData.getString(KEY_ENDPOINT) ?: return@withContext Result.failure()
        val jsonBody = inputData.getString(KEY_JSON) ?: return@withContext Result.failure()
        val secret = inputData.getString(KEY_SECRET) ?: ""

        try {
            val (code, errBody) = postJson(endpoint, jsonBody, secret)

            // Decide retry vs fail
            return@withContext when {
                code in 200..299 -> Result.success()
                code == 401 -> {
                    // Bad secret is not retryable.
                    Log.e(TAG, "Unauthorized (bad secret). Not retrying.")
                    Result.failure()
                }
                code in 400..499 -> {
                    // Most client errors aren't retryable.
                    Log.e(TAG, "Client error HTTP $code. Not retrying. body=${errBody ?: "(none)"}")
                    Result.failure()
                }
                else -> {
                    // 5xx or unknown -> retry.
                    Log.w(TAG, "Server error HTTP $code. Will retry. body=${errBody ?: "(none)"}")
                    Result.retry()
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Network error; will retry", e)
            return@withContext Result.retry()
        } catch (e: Exception) {
            // Unknown errors: retry a few times; WorkManager will stop after runAttemptCount cap
            Log.e(TAG, "Unexpected error; will retry", e)
            return@withContext Result.retry()
        }
    }

    private fun postJson(url: String, jsonBody: String, secret: String): Pair<Int, String?> {
        val conn = (URL(url).openConnection() as HttpURLConnection)
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Content-Type", "application/json")

            // Optional HMAC auth headers (recommended). We still send legacy JSON secret.
            if (secret.isNotBlank()) {
                val ts = (System.currentTimeMillis() / 1000L).toString()
                val sig = hmacSha256Hex(secret, "$ts.$jsonBody")
                conn.setRequestProperty("X-Timestamp", ts)
                conn.setRequestProperty("X-Signature", sig)
            }

            conn.doOutput = true
            conn.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val err = if (code !in 200..299) {
                try { conn.errorStream?.readBytes()?.toString(Charsets.UTF_8) } catch (_: Exception) { null }
            } else null

            return code to err
        } finally {
            conn.disconnect()
        }
    }

    private fun hmacSha256Hex(secret: String, message: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val out = mac.doFinal(message.toByteArray(Charsets.UTF_8))
        return out.joinToString("") { "%02x".format(it) }.lowercase(Locale.US)
    }

    companion object {
        private const val TAG = "SmsForwardWorker"

        const val KEY_ENDPOINT = "endpoint"
        const val KEY_JSON = "json"
        const val KEY_SECRET = "secret"

        fun inputData(endpoint: String, json: String, secret: String): Data =
            Data.Builder()
                .putString(KEY_ENDPOINT, endpoint)
                .putString(KEY_JSON, json)
                .putString(KEY_SECRET, secret)
                .build()

        val backoffPolicy: BackoffPolicy = BackoffPolicy.EXPONENTIAL
        val backoffDelay: Duration = Duration.ofSeconds(15)
    }
}
