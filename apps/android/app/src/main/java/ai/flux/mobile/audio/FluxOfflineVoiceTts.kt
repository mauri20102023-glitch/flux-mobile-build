package ai.flux.mobile.audio

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Closeable
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * FLUX VOICE 01 - optional Android offline neural-TTS bridge.
 *
 * This adapter uses ONLY an explicitly selected third-party Sherpa-ONNX
 * TextToSpeechService installed locally on the phone. If missing or not set up,
 * it FAILS CLOSED; it never silently changes to Google TTS or a paid cloud TTS.
 *
 * The external engine must include its own pt-BR model, e.g. a permitted
 * Sherpa-ONNX Kokoro or Piper packaging. The engine is not bundled in FLUX.
 */
class FluxOfflineVoiceTts(context: Context) : Closeable {
    companion object {
        const val ENGINE_PACKAGE = "com.k2fsa.sherpa.onnx.tts.engine"
        private val BRAZILIAN_PORTUGUESE = Locale.forLanguageTag("pt-BR")

        fun isInstalled(context: Context): Boolean {
            val query = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE).setPackage(ENGINE_PACKAGE)
            @Suppress("DEPRECATION")
            return context.packageManager.queryIntentServices(query, 0).isNotEmpty()
        }
    }

    private val initialized = CompletableDeferred<Boolean>()
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext, { status ->
        initialized.complete(status == TextToSpeech.SUCCESS)
    }, ENGINE_PACKAGE)

    suspend fun speak(text: String) {
        val phrase = text.trim()
        require(phrase.isNotEmpty()) { "A resposta da voz está vazia." }
        check(initialized.await()) {
            "Não foi possível iniciar o motor neural local. Verifique a instalação do Sherpa-ONNX."
        }
        check(tts.currentEngine == ENGINE_PACKAGE) {
            "O Android não iniciou o motor neural escolhido. A voz do sistema não será usada."
        }
        val availability = tts.isLanguageAvailable(BRAZILIAN_PORTUGUESE)
        check(availability >= TextToSpeech.LANG_AVAILABLE) {
            "Instale ou ative um modelo de português brasileiro no motor Sherpa-ONNX."
        }
        val languageResult = tts.setLanguage(BRAZILIAN_PORTUGUESE)
        check(languageResult >= TextToSpeech.LANG_AVAILABLE) {
            "O motor neural não conseguiu selecionar português brasileiro."
        }

        // Prefer an offline Brazilian voice provided by the installed engine.
        val candidates = runCatching { tts.voices.orEmpty() }.getOrDefault(emptySet())
        val portugueseVoice = candidates.firstOrNull {
            it.locale.language == "pt" && it.locale.country == "BR" && !it.isNetworkConnectionRequired
        }
        if (portugueseVoice != null) {
            check(tts.setVoice(portugueseVoice) == TextToSpeech.SUCCESS) {
                "A voz brasileira do motor neural não está disponível."
            }
        }

        suspendCancellableCoroutine<Unit> { continuation ->
            val utteranceId = "flux-local-" + UUID.randomUUID()
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String) = Unit
                override fun onDone(id: String) {
                    if (id == utteranceId && continuation.isActive) continuation.resume(Unit)
                }
                override fun onError(id: String) {
                    if (id == utteranceId && continuation.isActive) {
                        continuation.resumeWithException(IllegalStateException("A síntese local de voz falhou."))
                    }
                }
                override fun onError(id: String, errorCode: Int) = onError(id)
                override fun onStop(id: String, interrupted: Boolean) {
                    if (id == utteranceId && continuation.isActive) {
                        continuation.resumeWithException(IllegalStateException("A reprodução local foi interrompida."))
                    }
                }
            })
            continuation.invokeOnCancellation { tts.stop() }
            if (tts.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, utteranceId) != TextToSpeech.SUCCESS &&
                continuation.isActive
            ) {
                continuation.resumeWithException(IllegalStateException("O motor neural rejeitou a solicitação de áudio."))
            }
        }
    }

    fun stop() { tts.stop() }
    override fun close() { tts.stop(); tts.shutdown() }
}
