package com.example.myapplication

import android.content.Context

object AppConfig {
    private const val PREFS = "sms_bridge_prefs"

    private const val KEY_SERVER = "server_base_url"
    private const val KEY_SECRET = "bridge_secret" // legacy (optional fallback)
    private const val KEY_TOKEN = "auth_token"

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

    fun getAuthToken(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, "")!!
            .trim()

    fun setAuthToken(ctx: Context, token: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token.trim())
            .apply()
    }

    fun clearAuth(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_TOKEN)
            .apply()
    }

    // Legacy secret-based auth (optional fallback)
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
}
