package ai.flux.mobile.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import androidx.core.content.ContextCompat
import ai.flux.mobile.FluxApplication
import ai.flux.mobile.data.VoiceSessionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

/**
 * Native Gemini Live bridge. It prefers the server-issued short-lived credential.
 * Personal mode keeps Maurício's key encrypted with Android Keystore and reads
 * it only while opening the protected Gemini WebSocket.
 */
class FluxVoiceController(
    private val context: Context,
    private val onSessionChanged: (Boolean) -> Unit,
    private val onUserTranscript: (String) -> Unit,
    private val onAgentResponse: (String) -> Unit,
    private val onError: (String) -> Unit,
) : FluxVoiceBridge {
    private val application = context.applicationContext as FluxApplication
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val playbackExecutor = Executors.newSingleThreadExecutor()
    private val recording = AtomicBoolean(false)
    private val pendingMessages = ArrayDeque<String>()
    private var pendingContext: String? = null
    private var pendingFrame: ByteArray? = null
    private var pendingScreenPrompt: String? = null
    private var socket: WebSocket? = null
    private var liveSession: VoiceSessionResult? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var starting = false
    private var wanted = false
    private var setupComplete = false
    private var reconnectAttempts = 0
    private var reconnectJob: Job? = null
    private var lastSetupAt = 0L
    private var resumptionHandle: String? = null
    private var userTranscript = ""
    private var agentTranscript = ""

    override fun startSession() {
        if (wanted || starting || socket != null) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            onError("Autorize o microfone para conversar com o FLUX Live.")
            return
        }
        wanted = true
        starting = true
        reconnectAttempts = 0
        resumptionHandle = null
        onSessionChanged(true)
        scope.launch {
            runCatching { createSession() }
                .onSuccess { session ->
                    liveSession = session
                    reconnectAttempts = 0
                    connect(session)
                }
                .onFailure { failure -> fail(failure.message ?: "Não foi possível iniciar o FLUX Live.") }
        }
    }

    private fun connect(session: VoiceSessionResult) {
        val url = session.endpoint + "?" + session.authParameter + "=" +
            java.net.URLEncoder.encode(session.token, "UTF-8")
        socket = application.httpClient.newWebSocket(Request.Builder().url(url).build(), listener(session))
    }

    private suspend fun createSession(): VoiceSessionResult {
        if (application.connectionSettings.authTokenConfigured()) {
            runCatching { application.api.voiceSession() }.onSuccess { return it }
        }
        val personalKey = application.connectionSettings.geminiApiKey()
        if (personalKey.isNotBlank()) {
            return VoiceSessionResult(
                token = personalKey,
                endpoint = DIRECT_LIVE_ENDPOINT,
                model = "gemini-3.8-live",
                voice = "Orus",
                systemInstruction = PERSONAL_SYSTEM_INSTRUCTION,
                authParameter = "key",
            )
        }
        error("O Gemini Live não está ativo no Core. Ative uma chave Gemini pessoal nos ajustes.")
    }

    private fun listener(session: VoiceSessionResult) = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            scope.launch {
                if (!wanted || socket !== webSocket) {
                    webSocket.close(1000, "session-finished")
                    return@launch
                }
                starting = false
                webSocket.send(setupMessage(session).toString())
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            scope.launch {
                if (!wanted || socket !== webSocket) return@launch
                runCatching { handleMessage(JSONObject(text)) }
                    .onFailure { onError("O FLUX recebeu uma resposta de voz inválida.") }
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            scope.launch { handleDisconnect(webSocket, code, null) }
        }

        override fun onFailure(webSocket: WebSocket, throwable: Throwable, response: Response?) {
            scope.launch { handleDisconnect(webSocket, null, response?.code) }
        }
    }

    private fun handleDisconnect(webSocket: WebSocket, closeCode: Int?, httpCode: Int?) {
        // Callbacks from an old socket must not end a newer connection.
        if (socket !== webSocket) return
        socket = null
        if (setupComplete && android.os.SystemClock.elapsedRealtime() - lastSetupAt >= 30_000L) {
            reconnectAttempts = 0
        }
        setupComplete = false
        stopAudio()
        if (!wanted) return
        when {
            httpCode == 401 || httpCode == 403 -> fail("O Google recusou a chave de voz. Confira a chave Gemini nos Ajustes.")
            httpCode == 429 -> fail("O limite de uso do Gemini Live foi atingido. Tente novamente mais tarde.")
            httpCode == 400 || closeCode == 1008 -> {
                if (resumptionHandle != null) {
                    resumptionHandle = null
                    recoverOrStop()
                } else fail("O Gemini recusou a sessão de voz. Confira a chave e a configuração nos Ajustes.")
            }
            else -> recoverOrStop()
        }
    }

    private fun recoverOrStop() {
        if (!wanted) return
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            fail("A voz não conseguiu reconectar automaticamente. Verifique a internet e teste a chave Gemini nos Ajustes.")
            return
        }
        reconnectAttempts += 1
        starting = true
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(500L shl (reconnectAttempts - 1))
            if (!wanted) return@launch
            runCatching { createSession() }
                .onSuccess { session ->
                    if (wanted) {
                        liveSession = session
                        connect(session)
                    }
                }
                .onFailure { failure ->
                    if (!wanted) return@onFailure
                    if (failure.message?.contains("não está ativo", ignoreCase = true) == true) {
                        fail(failure.message ?: "A voz ainda não está configurada.")
                    } else recoverOrStop()
                }
        }
    }

    private fun setupMessage(session: VoiceSessionResult): JSONObject = JSONObject().apply {
        put("setup", JSONObject().apply {
            put("model", "models/${session.model}")
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().put("AUDIO"))
                put("speechConfig", JSONObject().apply {
                    put("voiceConfig", JSONObject().apply {
                        put("prebuiltVoiceConfig", JSONObject().put("voiceName", session.voice))
                    })
                })
            })
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", session.systemInstruction))))
            put("inputAudioTranscription", JSONObject())
            put("outputAudioTranscription", JSONObject())
            put("realtimeInputConfig", JSONObject().apply {
                put("automaticActivityDetection", JSONObject().apply {
                    put("disabled", false)
                    put("silenceDurationMs", 700)
                    put("prefixPaddingMs", 300)
                    put("startOfSpeechSensitivity", "START_SENSITIVITY_HIGH")
                    put("endOfSpeechSensitivity", "END_SENSITIVITY_HIGH")
                })
                put("activityHandling", "START_OF_ACTIVITY_INTERRUPTS")
                put("turnCoverage", "TURN_INCLUDES_ONLY_ACTIVITY")
            })
            put("sessionResumption", JSONObject().apply { resumptionHandle?.let { put("handle", it) } })
            put("contextWindowCompression", JSONObject().put("slidingWindow", JSONObject()))
        })
    }

    private fun handleMessage(message: JSONObject) {
        if (message.has("setupComplete")) {
            setupComplete = true
            lastSetupAt = android.os.SystemClock.elapsedRealtime()
            onSessionChanged(true)
            startRecording()
            pendingContext?.takeIf(String::isNotBlank)?.let(::sendContextNow)
            pendingContext = null
            sendPendingScreen()
            while (pendingMessages.isNotEmpty()) sendTextNow(pendingMessages.removeFirst(), true)
            return
        }
        message.optJSONObject("sessionResumptionUpdate")?.let { update ->
            if (update.optBoolean("resumable")) {
                update.optString("newHandle").takeIf(String::isNotBlank)?.let { resumptionHandle = it }
            } else resumptionHandle = null
        }
        val server = message.optJSONObject("serverContent")
        server?.optJSONObject("modelTurn")?.optJSONArray("parts")?.let { parts ->
            for (index in 0 until parts.length()) {
                parts.optJSONObject(index)?.optJSONObject("inlineData")?.optString("data")
                    ?.takeIf(String::isNotBlank)?.let(::playAudio)
            }
        }
        server?.optJSONObject("inputTranscription")?.optString("text")
            ?.takeIf(String::isNotBlank)?.let { userTranscript = mergeTranscript(userTranscript, it) }
        server?.optJSONObject("outputTranscription")?.optString("text")
            ?.takeIf(String::isNotBlank)?.let { agentTranscript = mergeTranscript(agentTranscript, it) }
        if (server?.optBoolean("interrupted") == true) flushPlayback()
        if (server?.optBoolean("turnComplete") == true) {
            val user = userTranscript.trim()
            val agent = agentTranscript.trim()
            userTranscript = ""
            agentTranscript = ""
            scope.launch {
                if (user.isNotEmpty()) onUserTranscript(user)
                if (agent.isNotEmpty()) onAgentResponse(agent)
            }
        }
        // GoAway warns that the connection will end soon. Keep the current
        // turn alive; the disconnect handler resumes it after the server closes.
    }

    private fun mergeTranscript(current: String, incoming: String): String = when {
        current.isBlank() -> incoming
        incoming.startsWith(current) -> incoming
        current.endsWith(incoming) -> current
        else -> current + incoming
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        if (recording.getAndSet(true)) return
        val minimum = AudioRecord.getMinBufferSize(INPUT_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            INPUT_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            max(minimum, INPUT_CHUNK_BYTES * 4),
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recording.set(false)
            recorder.release()
            fail("O microfone não pôde ser inicializado neste aparelho.")
            return
        }
        audioRecord = recorder
        recorder.startRecording()
        Thread({
            val buffer = ByteArray(INPUT_CHUNK_BYTES)
            while (recording.get()) {
                val count = recorder.read(buffer, 0, buffer.size)
                if (count > 0 && setupComplete) {
                    val encoded = Base64.encodeToString(buffer, 0, count, Base64.NO_WRAP)
                    val payload = JSONObject().put("realtimeInput", JSONObject().put(
                        "audio",
                        JSONObject().put("data", encoded).put("mimeType", "audio/pcm;rate=$INPUT_RATE"),
                    ))
                    socket?.send(payload.toString())
                }
            }
        }, "flux-live-microphone").apply { isDaemon = true; start() }
    }

    private fun playAudio(encoded: String) {
        val bytes = runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull() ?: return
        playbackExecutor.execute {
            runCatching {
                val track = audioTrack ?: createAudioTrack().also { audioTrack = it; it.play() }
                track.write(bytes, 0, bytes.size, AudioTrack.WRITE_BLOCKING)
            }
        }
    }

    private fun createAudioTrack(): AudioTrack {
        val minimum = AudioTrack.getMinBufferSize(OUTPUT_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        return AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(OUTPUT_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(max(minimum, OUTPUT_RATE * 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private fun flushPlayback() {
        playbackExecutor.execute {
            runCatching {
                audioTrack?.pause()
                audioTrack?.flush()
                audioTrack?.play()
            }
        }
    }

    override fun sendUserMessage(message: String) {
        val clean = message.trim()
        if (clean.isEmpty()) return
        if (setupComplete) sendTextNow(clean, true) else {
            pendingMessages.addLast(clean)
            startSession()
        }
    }

    private fun sendTextNow(message: String, complete: Boolean) {
        socket?.send(JSONObject().put("clientContent", JSONObject()
            .put("turns", JSONArray().put(JSONObject().put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", message)))))
            .put("turnComplete", complete)).toString())
    }

    override fun sendContextualUpdate(context: String) {
        val clean = context.trim()
        if (clean.isEmpty()) return
        if (setupComplete) sendContextNow(clean) else {
            pendingContext = clean
            startSession()
        }
    }

    private fun sendContextNow(value: String) = sendTextNow("Contexto atual da tela: $value", false)

    override fun sendScreenFrame(frame: Bitmap, prompt: String) {
        // A captura é recebida apenas após o pedido explícito do assistente.
        // Reduzir o tamanho evita travar a interface ou enfileirar uma imagem enorme.
        scope.launch(Dispatchers.Default) {
            val bytes = runCatching {
                val scale = minOf(1f, 1024f / max(frame.width, frame.height))
                val scaled = if (scale < 1f) Bitmap.createScaledBitmap(
                    frame, max(1, (frame.width * scale).toInt()),
                    max(1, (frame.height * scale).toInt()), true,
                ) else frame
                try {
                    ByteArrayOutputStream().use { output ->
                        check(scaled.compress(Bitmap.CompressFormat.JPEG, 75, output))
                        output.toByteArray()
                    }
                } finally {
                    if (scaled !== frame) scaled.recycle()
                }
            }.getOrElse {
                withContext(Dispatchers.Main.immediate) { onError("Não foi possível preparar a captura da tela.") }
                return@launch
            }
            withContext(Dispatchers.Main.immediate) {
                if (!wanted) return@withContext
                pendingFrame = bytes
                pendingScreenPrompt = prompt
                if (setupComplete) sendPendingScreen()
            }
        }
    }

    private fun sendPendingScreen() {
        val frame = pendingFrame ?: return
        val payload = JSONObject().put("realtimeInput", JSONObject().put("video", JSONObject()
            .put("data", Base64.encodeToString(frame, Base64.NO_WRAP))
            .put("mimeType", "image/jpeg")))
        if (socket?.send(payload.toString()) == true) {
            pendingFrame = null
            pendingScreenPrompt?.let { sendTextNow(it, true) }
            pendingScreenPrompt = null
        }
    }

    override fun endSession() {
        wanted = false
        reconnectJob?.cancel()
        reconnectJob = null
        starting = false
        setupComplete = false
        resumptionHandle = null
        pendingMessages.clear()
        pendingContext = null
        pendingFrame = null
        pendingScreenPrompt = null
        socket?.close(1000, "user-finished")
        socket = null
        stopAudio()
        onSessionChanged(false)
    }

    private fun stopAudio() {
        recording.set(false)
        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
        audioRecord = null
        if (!playbackExecutor.isShutdown) {
            runCatching {
                playbackExecutor.execute {
                    runCatching { audioTrack?.stop() }
                    runCatching { audioTrack?.release() }
                    audioTrack = null
                }
            }
        } else {
            runCatching { audioTrack?.release() }
            audioTrack = null
        }
    }

    private fun fail(message: String) {
        wanted = false
        reconnectJob?.cancel()
        reconnectJob = null
        starting = false
        setupComplete = false
        socket?.cancel()
        socket = null
        stopAudio()
        scope.launch {
            onSessionChanged(false)
            onError(message)
        }
    }

    override fun destroy() {
        wanted = false
        reconnectJob?.cancel()
        socket?.cancel()
        socket = null
        stopAudio()
        playbackExecutor.shutdownNow()
        scope.cancel()
    }

    private companion object {
        const val DIRECT_LIVE_ENDPOINT =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
        val PERSONAL_SYSTEM_INSTRUCTION = """
            Você é o FLUX, assistente pessoal de Maurício. Responda naturalmente em português do Brasil.
            Chame o usuário de Maurício quando isso soar natural. Dê o resultado primeiro, seja breve no simples
            e detalhado quando a tarefa exigir. Responda sobre qualquer assunto permitido, não apenas comandos
            objetivos. Entenda fala ditada, interrupções e autocorreções pela intenção. Use humor sutil quando
            combinar. Nunca finja ter executado uma ação externa; peça confirmação antes de enviar, comprar,
            publicar, apagar ou controlar dispositivos. Nunca peça senhas ou chaves durante uma conversa.
        """.trimIndent()
        const val INPUT_RATE = 16_000
        const val OUTPUT_RATE = 24_000
        const val INPUT_CHUNK_BYTES = 1_280
        const val MAX_RECONNECT_ATTEMPTS = 4
    }
}
