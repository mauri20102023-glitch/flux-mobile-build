package ai.flux.mobile.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import ai.flux.mobile.FluxApplication
import ai.flux.mobile.data.FluxLocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/** Turn-based FLUX voice: Android speech input, Cloudflare reasoning, FLUX ElevenLabs voice. */
class FluxConversationalVoice(
    private val context: Context,
    private val onSessionChanged: (Boolean) -> Unit,
    private val onUserTranscript: (String) -> Unit,
    private val onAgentResponse: (String) -> Unit,
    private val onError: (String) -> Unit,
) : FluxVoiceBridge {
    private val app = context.applicationContext as FluxApplication
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var recognizer: SpeechRecognizer? = null
    private var player: MediaPlayer? = null
    private var audioFile: File? = null
    private var job: Job? = null
    private var active = false
    private var busy = false
    private var contextNote = ""
    private val conversationId = UUID.randomUUID().toString()

    override fun startSession() {
        if (active) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            onError("Autorize o microfone para conversar com o FLUX.")
            return
        }
        if (!app.connectionSettings.authTokenConfigured()) {
            onError("Pareie o aparelho com o FLUX Core para usar a voz FLUX.")
            return
        }
        active = true
        onSessionChanged(true)
        listen()
    }

    private fun listen() {
        if (!active || busy) return
        recognizer?.destroy()
        val local = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        recognizer = if (local) SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            else SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
            override fun onResults(results: Bundle?) {
                val phrase = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.trim().orEmpty()
                recognizer?.destroy()
                recognizer = null
                if (phrase.isNotBlank()) process(phrase) else retryListen()
            }
            override fun onError(error: Int) {
                recognizer?.destroy()
                recognizer = null
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    this@FluxConversationalVoice.onError("O reconhecimento de voz precisa da permissão de microfone.")
                    endSession()
                } else retryListen()
            }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 700L)
        }
        runCatching { recognizer?.startListening(intent) }.onFailure {
            onError("Não consegui iniciar o reconhecimento de voz neste Android.")
            endSession()
        }
    }

    private fun retryListen() {
        if (active && !busy) scope.launch { delay(700); listen() }
    }

    private fun process(phrase: String) {
        if (!active) return
        recognizer?.destroy()
        recognizer = null
        player?.release()
        player = null
        busy = true
        onUserTranscript(phrase)
        job?.cancel()
        job = scope.launch {
            try {
                val facts = FluxLocalContext.collect(context, app.api, phrase)
                val response = app.api.chat(UUID.randomUUID().toString(), conversationId,
                    phrase, voice = true, context = listOf(contextNote, facts.context)
                        .filter(String::isNotBlank).joinToString("\n").take(3_000))
                contextNote = ""
                if (!active) return@launch
                onAgentResponse(response.content)
                val audio = app.api.synthesize(response.content.take(900))
                if (!active) return@launch
                play(audio)
            } catch (failure: Exception) {
                if (active) {
                    onError(failure.message ?: "Não consegui responder por voz agora.")
                    busy = false
                    retryListen()
                }
            }
        }
    }

    private fun play(audio: ByteArray) {
        val file = File.createTempFile("flux-voice-", ".mp3", context.cacheDir)
        file.writeBytes(audio)
        audioFile = file
        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener { finishPlayback() }
            setOnErrorListener { _, _, _ -> finishPlayback(); true }
            prepare()
            start()
        }
    }

    private fun finishPlayback() {
        player?.release()
        player = null
        audioFile?.delete()
        audioFile = null
        busy = false
        retryListen()
    }

    override fun sendUserMessage(message: String) {
        if (!active) startSession()
        if (active) process(message.trim())
    }

    override fun sendContextualUpdate(context: String) { contextNote = context.take(2_000) }

    override fun sendScreenFrame(frame: Bitmap, prompt: String) {
        onError("Esta versão da voz ainda não analisa imagens da tela. Use o texto visível do assistente.")
    }

    override fun endSession() {
        active = false
        busy = false
        job?.cancel()
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        player?.release()
        player = null
        audioFile?.delete()
        audioFile = null
        onSessionChanged(false)
    }

    override fun destroy() { endSession(); scope.cancel() }
}
