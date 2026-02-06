package com.example.myapplication

import android.content.Context

object AppConfig {
    private const val PREFS = "sms_bridge_prefs"
    private const val KEY_SERVER = "server_base_url"
    private const val KEY_SECRET = "bridge_secret"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_USE_HMAC_ONLY = "use_hmac_only"

    // Last Status keys
    private const val KEY_LAST_OK = "last_ok"
    private const val KEY_LAST_HTTP_CODE = "last_http_code"
    private const val KEY_LAST_ATTEMPT_MS = "last_attempt_ms"
    private const val KEY_LAST_ENDPOINT = "last_endpoint"
    private const val KEY_LAST_ERROR = "last_error"

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

    fun getAuthToken(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_AUTH_TOKEN, "")!!

    fun setAuthToken(ctx: Context, token: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_AUTH_TOKEN, token)
            .apply()
    }

    fun clearAuth(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_AUTH_TOKEN)
            .apply()
    }

    fun getUseHmacOnly(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_USE_HMAC_ONLY, false)

    fun setUseHmacOnly(ctx: Context, useHmacOnly: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_USE_HMAC_ONLY, useHmacOnly)
            .apply()
    }

    data class ForwardStatus(
        val hasData: Boolean,
        val ok: Boolean,
        val httpCode: Int,
        val attemptAtEpochMs: Long,
        val endpoint: String?,
        val errorBody: String?
    )

    fun getLastForwardStatus(ctx: Context): ForwardStatus {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val attemptMs = prefs.getLong(KEY_LAST_ATTEMPT_MS, 0L)
        if (attemptMs == 0L) {
            return ForwardStatus(false, false, 0, 0, null, null)
        }
        return ForwardStatus(
            hasData = true,
            ok = prefs.getBoolean(KEY_LAST_OK, false),
            httpCode = prefs.getInt(KEY_LAST_HTTP_CODE, 0),
            attemptAtEpochMs = attemptMs,
            endpoint = prefs.getString(KEY_LAST_ENDPOINT, null),
            errorBody = prefs.getString(KEY_LAST_ERROR, null)
        )
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
            .putBoolean(KEY_LAST_OK, httpCode in 200..299)
            .putInt(KEY_LAST_HTTP_CODE, httpCode)
            .putLong(KEY_LAST_ATTEMPT_MS, attemptAtEpochMs)
            .putString(KEY_LAST_ENDPOINT, endpoint)
            .putString(KEY_LAST_ERROR, errorBody)
            .apply()
    }
}
