package com.otohana.recorder

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Lightweight API client for the OtoHana Render backend.
 * All calls are suspend functions — call from a coroutine scope.
 */
object ApiClient {

    private const val TAG = "OtoHanaApi"

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    // ── Fetch remote config ───────────────────────────────────────────────────
    suspend fun fetchConfig(): AppConfig? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("${Constants.BASE_URL}/api/config")
                .get()
                .build()
            val body = http.newCall(req).execute().use { it.body?.string() } ?: return@withContext null
            val j = JSONObject(body)
            AppConfig(
                latestVersion    = j.optString("latestVersion", "1.0.0"),
                forceUpdate      = j.optBoolean("forceUpdate", false),
                defaultWatermark = j.optString("defaultWatermark", Constants.DEFAULT_WATERMARK_TEXT),
                logoLocked       = j.optBoolean("logoWatermarkLock", true),
                allowedBitrates  = j.optJSONArray("allowedBitrates")
                    ?.let { arr -> (0 until arr.length()).map { arr.getInt(it) } }
                    ?: listOf(2, 4, 8, 16)
            )
        } catch (e: Exception) {
            Log.w(TAG, "fetchConfig failed: $e")
            null
        }
    }

    // ── Log a completed recording ─────────────────────────────────────────────
    suspend fun logRecording(
        deviceId:        String,
        durationSeconds: Long,
        bitrateKbps:     Int,
        audioMode:       String,
        hasWatermark:    Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("deviceId",        deviceId)
                put("durationSeconds", durationSeconds)
                put("bitrateKbps",     bitrateKbps)
                put("audioMode",       audioMode)
                put("hasWatermark",    hasWatermark)
            }.toString()

            val req = Request.Builder()
                .url("${Constants.BASE_URL}/api/recordings/log")
                .post(payload.toRequestBody(JSON))
                .build()

            val code = http.newCall(req).execute().use { it.code }
            code in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "logRecording failed: $e")
            false
        }
    }
}

// ── Data class ────────────────────────────────────────────────────────────────
data class AppConfig(
    val latestVersion:    String,
    val forceUpdate:      Boolean,
    val defaultWatermark: String,
    val logoLocked:       Boolean,
    val allowedBitrates:  List<Int>
)
