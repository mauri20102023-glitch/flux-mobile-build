package ai.flux.mobile.audio

import android.content.Context
import ai.flux.mobile.FluxApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID

interface FluxTextBridge {
    fun send(message: String)
    fun destroy()
}

/** Text chat through FLUX Core and the same Gemini identity used by FLUX Live. */
class FluxTextController(
    context: Context,
    private val onResponse: (String) -> Unit,
    private val onError: (String) -> Unit,
) : FluxTextBridge {
    private val api = (context.applicationContext as FluxApplication).api
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val conversationId = "android-${UUID.randomUUID()}"

    override fun send(message: String) {
        val clean = message.trim()
        if (clean.isEmpty()) return
        scope.launch {
            runCatching {
                api.chat(
                    requestId = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    message = clean,
                    voice = false,
                )
            }.onSuccess { onResponse(it.content) }
                .onFailure { onError(it.message ?: "Não foi possível conversar com o FLUX.") }
        }
    }

    override fun destroy() = scope.cancel()
}
