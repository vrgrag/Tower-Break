package com.towerbreak.towerbreakgame.beacon

import com.towerbreak.towerbreakgame.cache.BastionSpec
import com.towerbreak.towerbreakgame.cache.BastionResult
import com.towerbreak.towerbreakgame.trail.Trace
import com.towerbreak.towerbreakgame.trail.UrlGuard
import com.towerbreak.towerbreakgame.trail.UserAgent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Config endpoint client. One responsibility, one method: POST the attribution
 * body and return the parsed answer.
 *
 * The URL out of a successful response is checked against [UrlGuard] before it
 * is handed back. A destination outside the allowlist is treated the same as
 * `ok:false` — the app opens the native part, and the mode is not persisted
 * (the endpoint did answer, but its answer was rejected by our own gate, so
 * the "did the server rule on this install" question is still open next launch).
 */
class BastionClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(BastionSpec.configTimeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(BastionSpec.configTimeoutMs, TimeUnit.MILLISECONDS)
        .build()

    private val json = "application/json; charset=utf-8".toMediaType()

    suspend fun fetchChannel(body: JSONObject): BastionResult = withContext(Dispatchers.IO) {
        val endpoint = BastionSpec.resolveConfigEndpoint()
        if (endpoint.isBlank()) {
            Trace.w(TAG, "endpoint is blank — nobody to ask")
            return@withContext BastionResult.unreachable()
        }
        Trace.i(TAG, "POST config endpoint")
        try {
            val req = Request.Builder()
                .url(endpoint)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", UserAgent.value)
                .post(body.toString().toRequestBody(json))
                .build()

            http.newCall(req).execute().use { resp ->
                val code = resp.code
                val raw = resp.body?.string().orEmpty()
                Trace.i(TAG, "HTTP $code (${raw.length} chars)")

                if (code == 404) return@withContext BastionResult.native()
                if (code !in 200..299) return@withContext BastionResult.native()
                parseResponse(raw)
            }
        } catch (e: Exception) {
            Trace.w(TAG, "request never landed: ${e.message}")
            BastionResult.unreachable()
        }
    }

    private fun parseResponse(raw: String): BastionResult {
        if (raw.isBlank()) return BastionResult.native()
        return try {
            val j = JSONObject(raw)
            val ok = j.optBoolean("ok", false)
            val url = j.optString("url", "")
            val exp = j.optLong("expires", 0L)
            if (ok && url.isNotBlank()) {
                if (!UrlGuard.accepts(url)) {
                    // The server named a destination; our own gate refused it. That
                    // is an allowlist misconfiguration on our side, not a ruling on
                    // this install, so the decision stays open for the next launch —
                    // otherwise a missing host permanently strands paid installs.
                    Trace.w(TAG, "endpoint URL rejected by allowlist")
                    return BastionResult.unreachable()
                }
                BastionResult.stream(url, exp)
            } else {
                BastionResult.native()
            }
        } catch (e: Exception) {
            Trace.w(TAG, "JSON parse error: ${e.message}")
            BastionResult.native()
        }
    }

    private companion object { const val TAG = "BastionClient" }
}
