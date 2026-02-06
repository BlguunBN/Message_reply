package com.example.myapplication

import android.content.Context

object AppConfig {
    private const val PREFS = "sms_bridge_prefs"

    // Settings
    private const val KEY_SERVER = "server_base_url"
    private const val KEY_SECRET = "bridge_secret"
    private const val KEY_USE_HMAC_ONLY = "use_hmac_only"

    // Debug/status (last forward attempt)
    private const val KEY_LAST_ENDPOINT = "last_endpoint"
    private const val KEY_LAST_ATTEMPT_AT_EPOCH_MS = "last_attempt_at_epoch_ms"
    private const val KEY_LAST_HTTP_CODE = "last_http_code"
    private const val KEY_LAST_ERROR_BODY = "last_error_body"

    fun getServerBaseUrl(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SERVER, "http://10.0.2.2:3000")!!
            .trim()
            .trimEnd('/')

    fun setServerBaseUrl(ctx: Context, url: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SERVER, url.trim())
            .apply()
    }

    fun getSecret(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SECRET, "")!!
            .trim()

    fun setSecret(ctx: Context, secret: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SECRET, secret.trim())
            .apply()
    }

    fun getUseHmacOnly(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_USE_HMAC_ONLY, false)

    fun setUseHmacOnly(ctx: Context, enabled: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_USE_HMAC_ONLY, enabled)
            .apply()
    }

    fun recordLastForwardAttempt(
        ctx: Context,
        endpoint: String,
        attemptAtEpochMs: Long,
        httpCode: Int,
        errorBody: String?
    ) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_ENDPOINT, endpoint)
            .putLong(KEY_LAST_ATTEMPT_AT_EPOCH_MS, attemptAtEpochMs)
            .putInt(KEY_LAST_HTTP_CODE, httpCode)
            .putString(KEY_LAST_ERROR_BODY, errorBody)
            .apply()
    }

    data class LastForwardStatus(
        val endpoint: String?,
        val attemptAtEpochMs: Long,
        val httpCode: Int,
        val errorBody: String?
    ) {
        val ok: Boolean get() = httpCode in 200..299
        val hasData: Boolean get() = attemptAtEpochMs > 0L || (endpoint?.isNotBlank() == true)
    }

    fun getLastForwardStatus(ctx: Context): LastForwardStatus {
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return LastForwardStatus(
            endpoint = sp.getString(KEY_LAST_ENDPOINT, null),
            attemptAtEpochMs = sp.getLong(KEY_LAST_ATTEMPT_AT_EPOCH_MS, 0L),
            httpCode = sp.getInt(KEY_LAST_HTTP_CODE, 0),
            errorBody = sp.getString(KEY_LAST_ERROR_BODY, null)
        )
    }
}

