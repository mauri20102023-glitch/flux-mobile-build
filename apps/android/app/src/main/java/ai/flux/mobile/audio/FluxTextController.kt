package ai.flux.mobile.audio

import android.content.Context
import ai.flux.mobile.FluxApplication
import ai.flux.mobile.model.Role
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

interface FluxTextBridge {
    fun send(message: String, context: String = "")
    fun destroy()
}

/** Text chat uses the protected personal Gemini key when available. */
class FluxTextController(
    context: Context,
    private val onResponse: (String) -> Unit,
    private val onError: (String) -> Unit,
) : FluxTextBridge {
    private val application = context.applicationContext as FluxApplication
    private val api = application.api
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sendMutex = Mutex()

    override fun send(message: String, context: String) {
        val clean = message.trim()
        if (clean.isEmpty()) return
        scope.launch {
            sendMutex.withLock {
                runCatching {
                    if (application.connectionSettings.authTokenConfigured()) {
                        runCatching { api.chat(
                            requestId = UUID.randomUUID().toString(),
                            conversationId = if (application.workspace.memoryEnabled())
                                application.cache.conversationId() else UUID.randomUUID().toString(),
                            message = clean,
                            voice = false,
                            context = context,
                        ).content
                    } else if (application.connectionSettings.geminiApiKeyConfigured()) {
                        personalChat()
                    } else {
                        error("Pareie o aparelho com o FLUX Core ou ative a chave Gemini pessoal.")
                    }
                }.onSuccess(onResponse)
                    .onFailure { onError(it.message ?: "Não foi possível conversar com o FLUX.") }
            }
        }
    }

    private suspend fun personalChat(): String = withContext(Dispatchers.IO) {
        val key = application.connectionSettings.geminiApiKey()
        require(key.isNotBlank()) { "Ative sua chave do Gemini nos ajustes do FLUX." }
        // beginDirectMessage já salvou a mensagem atual no histórico local.
        val history = JSONArray()
        val messages = application.cache.loadMessages()
        val context = if (application.workspace.memoryEnabled()) messages.takeLast(16)
            .dropWhile { it.role != Role.USER } else messages.takeLast(1)
        context.forEach { message ->
            val role = when (message.role) {
                Role.USER -> "user"
                Role.FLUX -> "model"
            }
            history.put(JSONObject().put("role", role)
                .put("parts", JSONArray().put(JSONObject().put("text", message.content))))
        }
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put(
                "text",
                "Você é o FLUX, assistente pessoal de Maurício. Responda naturalmente em português do Brasil. " +
                    "Seja útil e claro. Nunca diga que executou ações externas que não executou.",
            ))))
            .put("contents", history)
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent")
            .header("x-goog-api-key", key)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        application.httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error(when (response.code) {
                    401, 403 -> "A chave do Gemini foi recusada. Confira a chave nos ajustes."
                    429 -> "O limite de uso do Gemini foi atingido. Tente novamente mais tarde."
                    else -> "O Gemini não respondeu (erro ${response.code})."
                })
            }
            val candidates = JSONObject(response.body?.string().orEmpty()).optJSONArray("candidates")
            val parts = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
            val answer = buildString {
                if (parts != null) for (index in 0 until parts.length()) {
                    parts.optJSONObject(index)?.takeUnless { it.optBoolean("thought") }
                        ?.optString("text")?.takeIf(String::isNotBlank)?.let(::append)
                }
            }.trim()
            answer.ifBlank { error("O Gemini respondeu sem texto. Tente novamente.") }
        }
    }

    override fun destroy() = scope.cancel()
}
