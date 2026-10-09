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
data class WeatherResult(val temperature: Double, val high: Double, val low: Double,
    val rainChance: Int, val condition: String)
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
    suspend fun workspace(path: String, body: JSONObject? = null, method: String = if (body == null) "GET" else "POST"): JSONObject = withContext(Dispatchers.IO) {
        require(path.startsWith("/v1/"))
        val builder = request(path)
        if (method == "GET") builder.get() else builder.method(method, if (method == "DELETE") null else json(body ?: JSONObject()))
        executeWithRetry(builder.build(), 1).use {
            val raw = it.body?.string().orEmpty()
            if (!it.isSuccessful) throw apiFailure(it.code, raw)
            JSONObject(raw)
        }
    }

    suspend fun realtimeConfig(): JSONObject = withContext(Dispatchers.IO) {
        executeWithRetry(request("/v1/live/ice-servers").get().build(), 2).use {
            val raw = it.body?.string().orEmpty()
            if (!it.isSuccessful) throw apiFailure(it.code, raw)
            JSONObject(raw)
        }
    }

    suspend fun realtimeOffer(sdp: String): String = withContext(Dispatchers.IO) {
        executeWithRetry(request("/v1/live/offer").post(sdp.toRequestBody("application/sdp".toMediaType())).build(), 1).use {
            val raw = it.body?.string().orEmpty()
            if (!it.isSuccessful) throw apiFailure(it.code, raw)
            raw
        }
    }

    suspend fun analyzeImage(image: String, prompt: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject().put("image", image).put("prompt", prompt)
        executeWithRetry(request("/v1/vision").post(json(body)).build(), 1).use {
            val raw = it.body?.string().orEmpty()
            if (!it.isSuccessful) throw apiFailure(it.code, raw)
            JSONObject(raw).getString("content")
        }
    }
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

    suspend fun migrateLegacy(legacyToken: String): PairResult = withContext(Dispatchers.IO) {
        val body = JSONObject().put("deviceId", deviceId()).put("legacyToken", legacyToken)
        val request = Request.Builder()
            .url("https://flux-mobile-build2.mauri20102023.workers.dev/v1/pair/migrate")
            .post(json(body)).build()
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
        context: String = "",
        mode: String = "STANDARD",
    ): ChatResult = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("requestId", requestId)
            put("conversationId", conversationId)
            put("message", message)
            put("deviceId", deviceId())
            put("modality", if (voice) "VOICE" else "TEXT")
            put("mode", mode)
            if (context.isNotBlank()) put("context", context.take(3_000))
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

    suspend fun weather(latitude: Double, longitude: Double): WeatherResult = withContext(Dispatchers.IO) {
        val url = "/v1/weather?lat=${"%.3f".format(java.util.Locale.US, latitude)}" +
            "&lon=${"%.3f".format(java.util.Locale.US, longitude)}"
        executeWithRetry(request(url).get().build(), maxAttempts = 2).use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw apiFailure(response.code, raw)
            val data = JSONObject(raw)
            val code = data.optInt("code", -1)
            val condition = when {
                code == 0 -> "céu limpo"
                code in 1..3 -> "parcialmente nublado"
                code == 45 || code == 48 -> "neblina"
                code in 51..67 || code in 80..82 -> "chuva"
                code in 71..77 -> "neve"
                code >= 95 -> "trovoadas"
                else -> "condições variáveis"
            }
            WeatherResult(data.getDouble("temperature"), data.optDouble("high"),
                data.optDouble("low"), data.optInt("rainChance"), condition)
        }
    }

    suspend fun updateCalendar(events: List<CalendarEntry>) = withContext(Dispatchers.IO) {
        val entries = org.json.JSONArray()
        events.take(20).forEach { entry ->
            entries.put(JSONObject().put("title", entry.title).put("start", entry.start).put("end", entry.end))
        }
        val body = JSONObject().put("events", entries)
        executeWithRetry(request("/v1/calendar").post(json(body)).build(), maxAttempts = 1).use {
            if (!it.isSuccessful) throw apiFailure(it.code, it.body?.string().orEmpty())
        }
    }

    suspend fun synthesize(text: String): ByteArray = withContext(Dispatchers.IO) {
        val body = JSONObject().put("text", text.take(1_000))
        executeWithRetry(request("/v1/tts").post(json(body)).build(), maxAttempts = 1).use {
            if (!it.isSuccessful) throw apiFailure(it.code, it.body?.string().orEmpty())
            it.body?.bytes() ?: error("A voz FLUX retornou áudio vazio.")
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
        mode: String = "STANDARD",
    ): ChatResult = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("conversationId", conversationId)
            put("message", message)
            put("deviceId", deviceId())
            put("modality", if (voice) "VOICE" else "TEXT")
            put("mode", mode)
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
                if (continuation.isActive) continuation.resume(response) else response.close()
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
