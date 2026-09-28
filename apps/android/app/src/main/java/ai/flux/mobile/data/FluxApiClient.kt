package ai.flux.mobile.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class ChatResult(val content: String, val mode: String, val outputDeviceId: String)
data class PairResult(val deviceToken: String, val deviceId: String)
data class VoiceSessionResult(
    val token: String,
    val endpoint: String,
    val model: String,
    val voice: String,
    val systemInstruction: String,
    val authParameter: String = "access_token",
)
data class DiagnosticsResult(
    val coreOk: Boolean,
    val aiReady: Boolean,
    val activationRequired: Boolean,
    val voiceConfigured: Boolean,
    val voiceProvider: String,
    val voiceOfficial: Boolean,
)

class FluxApiException(
    val statusCode: Int,
    val retryable: Boolean,
    message: String,
) : IOException(message)

class FluxApiClient(
    private val client: OkHttpClient,
    private val baseUrl: () -> String,
    private val authToken: () -> String,
    private val deviceId: () -> String,
) {
    suspend fun redeemPairingCode(code: String): PairResult = withContext(Dispatchers.IO) {
        val id = deviceId()
        val body = JSONObject().apply {
            put("deviceId", id)
            put("deviceName", "FLUX Mobile de Maurício")
            put("code", code.trim())
        }
        val request = Request.Builder()
            .url(baseUrl().trimEnd('/') + "/v1/pair/redeem")
            .post(json(body))
            .build()
        executeWithRetry(request, maxAttempts = 1).use {
            val raw = it.body?.string().orEmpty()
            if (!it.isSuccessful) throw apiFailure(it.code, raw)
            val result = JSONObject(raw)
            PairResult(result.getString("deviceToken"), result.getString("deviceId"))
        }
    }

    suspend fun diagnostics(): DiagnosticsResult = withContext(Dispatchers.IO) {
        val response = executeWithRetry(request("/v1/diagnostics").get().build())
        response.use {
            val raw = it.body?.string().orEmpty()
            if (!it.isSuccessful) throw apiFailure(it.code, raw)
            val responseBody = JSONObject(raw)
            val root = responseBody.getJSONObject("diagnostics")
            val voiceProfile = responseBody.optJSONObject("voiceProfile")
            val voiceStatus = root.optString("voice")
            val aiReady = (responseBody.optJSONObject("aiProfile") ?: responseBody.optJSONObject("localAi"))?.optBoolean("ready")
                ?: (root.optString("ai") == "OK")
            val voiceConfigured = voiceStatus == "OK" || voiceStatus == "FALLBACK"
            DiagnosticsResult(
                coreOk = root.optString("core") == "OK",
                aiReady = aiReady,
                activationRequired = responseBody.optBoolean("activationRequired", !aiReady),
                voiceConfigured = voiceConfigured,
                voiceProvider = voiceProfile?.optString("provider", "unavailable") ?: "unavailable",
                voiceOfficial = voiceConfigured && (voiceProfile?.optBoolean("official", false) ?: false),
            )
        }
    }

    suspend fun registerMobile() = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("deviceId", deviceId())
            put("deviceType", "MOBILE")
            put("name", "Flux Mobile")
            put("platform", "Android")
            put("capabilities", JSONArrayBuilder.of("DISPLAY", "AUDIO_INPUT", "AUDIO_OUTPUT", "NOTIFICATIONS"))
            put("online", true)
            put("lastSeen", java.time.Instant.now().toString())
            put("audioInput", true)
            put("audioOutput", true)
            put("display", true)
            put("trustLevel", "PAIRED")
        }
        executeWithRetry(request("/v1/devices/register").post(json(body)).build()).use {
            val raw = it.body?.string().orEmpty()
            if (!it.isSuccessful) throw apiFailure(it.code, raw)
        }
    }

    suspend fun chat(
        requestId: String,
        conversationId: String,
        message: String,
        voice: Boolean,
    ): ChatResult = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("requestId", requestId)
            put("conversationId", conversationId)
            put("message", message)
            put("deviceId", deviceId())
            put("modality", if (voice) "VOICE" else "TEXT")
        }
        val response = executeWithRetry(request("/v1/chat").post(json(body)).build(), maxAttempts = 4)
        response.use {
            val raw = it.body?.string().orEmpty()
            if (!it.isSuccessful) throw apiFailure(it.code, raw)
            val result = JSONObject(raw)
            ChatResult(
                content = result.getString("content"),
                mode = result.getString("mode"),
                outputDeviceId = result.optString("outputDeviceId", deviceId()),
            )
        }
    }

    suspend fun generateImage(prompt: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply { put("prompt", prompt) }
        executeWithRetry(request("/v1/images").post(json(body)).build(), maxAttempts = 2).use {
            val raw = it.body?.string().orEmpty()
            if (!it.isSuccessful) throw apiFailure(it.code, raw)
            JSONObject(raw).getString("image")
        }
    }

    suspend fun streamChat(
        conversationId: String,
        message: String,
        voice: Boolean,
        onDelta: (String) -> Unit,
    ): ChatResult = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("conversationId", conversationId)
            put("message", message)
            put("deviceId", deviceId())
            put("modality", if (voice) "VOICE" else "TEXT")
        }
        val response = execute(request("/v1/chat/stream").post(json(body)).build())
        response.use {
            if (!it.isSuccessful) throw apiFailure(it.code, it.body?.string().orEmpty())
            val source = it.body?.source() ?: error("Resposta vazia")
            var event = ""
            var final: ChatResult? = null
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                when {
                    line.startsWith("event: ") -> event = line.removePrefix("event: ")
                    line.startsWith("data: ") -> {
                        val payload = JSONObject(line.removePrefix("data: "))
                        when (event) {
                            "delta" -> onDelta(payload.optString("content"))
                            "final" -> {
                                val result = payload.getJSONObject("response")
                                final = ChatResult(
                                    content = result.getString("content"),
                                    mode = result.getString("mode"),
                                    outputDeviceId = result.getString("outputDeviceId"),
                                )
                            }
                            "error" -> error(payload.optString("message", "Falha temporária"))
                        }
                    }
                }
            }
            final ?: error("Resposta encerrada sem confirmação final")
        }
    }

    suspend fun voiceSession(): VoiceSessionResult = withContext(Dispatchers.IO) {
        val request = request("/v1/live/session")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()
        executeWithRetry(request, maxAttempts = 3).use {
            val raw = it.body?.string().orEmpty()
            if (!it.isSuccessful) throw apiFailure(it.code, raw)
            val result = JSONObject(raw)
            VoiceSessionResult(
                token = result.getString("token"),
                endpoint = result.getString("endpoint"),
                model = result.getString("model"),
                voice = result.optString("voice", "Orus"),
                systemInstruction = result.getString("systemInstruction"),
            )
        }
    }

    private fun request(path: String): Request.Builder = Request.Builder()
        .url(baseUrl().trimEnd('/') + path)
        .header("X-Flux-User-Id", "mauricio")
        .header("X-Flux-Device-Id", deviceId())
        .apply {
            authToken().takeIf(String::isNotBlank)?.let { header("Authorization", "Bearer $it") }
        }

    private fun json(value: JSONObject) = value.toString().toRequestBody("application/json".toMediaType())

    private fun apiFailure(statusCode: Int, rawBody: String): FluxApiException {
        val serverMessage = runCatching {
            val payload = JSONObject(rawBody)
            payload.optString("message").ifBlank { payload.optString("error") }
        }
            .getOrNull()
            .orEmpty()
            .ifBlank {
                when (statusCode) {
                    401 -> "O aplicativo não foi autorizado pelo FLUX Core."
                    429 -> "A IA atingiu o limite de uso. Tente novamente em instantes."
                    503 -> "A inteligência e a voz ainda precisam ser ativadas."
                    else -> "O FLUX Core respondeu com erro ${statusCode}."
                }
            }
        return FluxApiException(
            statusCode = statusCode,
            retryable = statusCode == 408 || statusCode == 409 || statusCode == 429 || statusCode in 500..599,
            message = serverMessage,
        )
    }

    private suspend fun execute(request: Request): Response = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, exception: IOException) {
                if (continuation.isActive) continuation.resumeWithException(exception)
            }
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }
        })
    }

    private suspend fun executeWithRetry(request: Request, maxAttempts: Int = 3): Response {
        var lastFailure: Throwable? = null
        repeat(maxAttempts) { attempt ->
            try {
                val response = execute(request)
                if (!response.isRetryable() || attempt == maxAttempts - 1) return response
                response.close()
            } catch (failure: IOException) {
                lastFailure = failure
                if (attempt == maxAttempts - 1) throw failure
            }
            delay(500L * (1L shl attempt))
        }
        throw lastFailure ?: IOException("Flux Core indisponível")
    }

    private fun Response.isRetryable(): Boolean = code == 408 || code == 409 || code == 429 || code in 500..599
}

private object JSONArrayBuilder {
    fun of(vararg values: String) = org.json.JSONArray().apply { values.forEach(::put) }
}
