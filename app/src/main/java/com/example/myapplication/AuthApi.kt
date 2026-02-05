package com.example.myapplication

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AuthApi {

    data class AuthResult(
        val ok: Boolean,
        val token: String?,
        val error: String?
    )

    fun signup(baseUrl: String, username: String, email: String, password: String): AuthResult {
        val url = "$baseUrl/auth/signup"
        val json = JSONObject().apply {
            put("username", username)
            put("email", email)
            put("password", password)
        }
        return postJson(url, json)
    }

    fun login(baseUrl: String, identifier: String, password: String): AuthResult {
        val url = "$baseUrl/auth/login"
        val json = JSONObject().apply {
            put("identifier", identifier)
            put("password", password)
        }
        return postJson(url, json)
    }

    private fun postJson(url: String, json: JSONObject): AuthResult {
        val conn = (URL(url).openConnection() as HttpURLConnection)
        return try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val body = json.toString()
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val bytes = if (code in 200..299) conn.inputStream.readBytes() else (conn.errorStream?.readBytes() ?: ByteArray(0))
            val text = bytes.toString(Charsets.UTF_8)

            if (code !in 200..299) {
                return AuthResult(ok = false, token = null, error = "HTTP $code: $text")
            }

            val resp = JSONObject(text)
            val token = resp.optString("token", null)
            AuthResult(ok = resp.optBoolean("ok", false), token = token, error = null)
        } catch (e: Exception) {
            AuthResult(ok = false, token = null, error = e.toString())
        } finally {
            conn.disconnect()
        }
    }
}
