package ai.flux.mobile.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.util.Base64
import androidx.core.content.ContextCompat
import ai.flux.mobile.FluxApplication
import ai.flux.mobile.data.FluxLocalContext
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Full-duplex audio. WebRTC owns AEC; provider VAD cancels speech on barge-in. */
class FluxRealtimeVoice(
    private val context: Context,
    private val onSessionChanged: (Boolean) -> Unit,
    private val onUserTranscript: (String) -> Unit,
    private val onAgentResponse: (String) -> Unit,
    private val onError: (String) -> Unit,
) : FluxVoiceBridge {
    private val app = context.applicationContext as FluxApplication
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var previousMode = AudioManager.MODE_NORMAL
    private var previousSpeaker = false
    private var factory: PeerConnectionFactory? = null
    private var audioModule: JavaAudioDeviceModule? = null
    private var source: AudioSource? = null
    private var track: AudioTrack? = null
    private var peer: PeerConnection? = null
    private var channel: DataChannel? = null
    private var connectionJob: Job? = null
    private var visionJob: Job? = null
    private var active = false
    private var ready = false
    private var epoch = 0
    private var config = JSONObject()
    private var transcript = StringBuilder()
    private var pendingText: String? = null
    private var currentImage: String? = null
    private var contextNote = ""
    private var levelListener: ((Float)->Unit)? = null
    private var lastLevelAt=0L
    override fun setAudioLevelListener(listener:(Float)->Unit) {levelListener=listener}

    override fun startSession() {
        if (active) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            onError("Autorize o microfone para conversar com o FLUX."); return
        }
        if (!app.connectionSettings.authTokenConfigured()) { onError("Pareie este aparelho com o Core."); return }
        active = true
        val sessionEpoch = ++epoch
        previousMode = audioManager.mode
        previousSpeaker = audioManager.isSpeakerphoneOn
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true
        connectionJob = scope.launch {
            try {
                config = app.api.realtimeConfig()
                if (!active || sessionEpoch != epoch) return@launch
                synchronized(FluxRealtimeVoice::class.java) {
                    if (!initialized) {
                        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context.applicationContext).createInitializationOptions())
                        initialized = true
                    }
                }
                audioModule = JavaAudioDeviceModule.builder(context.applicationContext)
                    .setUseHardwareAcousticEchoCanceler(true).setUseHardwareNoiseSuppressor(true)
                    .setSamplesReadyCallback { samples ->
                        val now=android.os.SystemClock.elapsedRealtime()
                        if(now-lastLevelAt>=100 && samples.audioFormat==android.media.AudioFormat.ENCODING_PCM_16BIT){
                            lastLevelAt=now
                            val level=fluxMicLevel(samples.data)
                            scope.launch {if(active&&sessionEpoch==epoch)levelListener?.invoke(level)}
                        }
                    }.createAudioDeviceModule()
                factory = PeerConnectionFactory.builder().setAudioDeviceModule(audioModule).createPeerConnectionFactory()
                val ice = mutableListOf<PeerConnection.IceServer>()
                val servers = config.optJSONArray("iceServers") ?: JSONArray()
                for (i in 0 until servers.length()) {
                    val item = servers.getJSONObject(i)
                    val urls = item.opt("urls")
                    val list = if (urls is JSONArray) (0 until urls.length()).map { urls.getString(it) }
                        else listOf(item.optString("urls")).filter(String::isNotBlank)
                    if (list.isNotEmpty()) ice += PeerConnection.IceServer.builder(list)
                        .setUsername(item.optString("username")).setPassword(item.optString("credential")).createIceServer()
                }
                val rtc = PeerConnection.RTCConfiguration(ice).apply { sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN }
                peer = factory!!.createPeerConnection(rtc, observer(sessionEpoch)) ?: error("Não consegui abrir o áudio WebRTC.")
                source = factory!!.createAudioSource(MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
                })
                track = factory!!.createAudioTrack("flux-mic", source)
                peer!!.addTrack(track, listOf("flux-audio"))
                channel = peer!!.createDataChannel("oai-events", DataChannel.Init())
                channel!!.registerObserver(object : DataChannel.Observer {
                    override fun onBufferedAmountChange(previousAmount: Long) = Unit
                    override fun onStateChange() { scope.launch {
                        if (active && sessionEpoch == epoch && channel?.state() == DataChannel.State.OPEN) configureSession()
                    } }
                    override fun onMessage(buffer: DataChannel.Buffer) {
                        if (buffer.binary) return
                        val bytes = ByteArray(buffer.data.remaining()); buffer.data.get(bytes)
                        scope.launch { if (active && sessionEpoch == epoch) handleEvent(String(bytes, Charsets.UTF_8)) }
                    }
                })
                val offer = createOffer(peer!!)
                setDescription(peer!!, offer, true)
                withTimeoutOrNull(3500) { while (peer?.iceGatheringState() != PeerConnection.IceGatheringState.COMPLETE) delay(40) }
                val answer = app.api.realtimeOffer(peer!!.localDescription.description)
                if (!active || sessionEpoch != epoch) return@launch
                setDescription(peer!!, SessionDescription(SessionDescription.Type.ANSWER, answer), false)
            } catch (failure: Exception) {
                if (failure !is CancellationException && active && sessionEpoch == epoch) {
                    onError(failure.message ?: "A conexão de voz falhou. Feche e tente novamente.")
                    endSession()
                }
            }
        }
    }

    private fun configureSession() {
        send(JSONObject().put("type", "session.update").put("session", JSONObject().apply {
            put("model", config.getString("model"))
            put("instructions", config.getString("instructions") + "\nNão presuma acesso a contas, tela ou ações externas. Use somente contexto realmente fornecido.")
            put("max_output_tokens", 800)
            put("output_modalities", JSONArray().put("audio").put("text"))
            put("audio", JSONObject().put("input", JSONObject()
                .put("transcription", JSONObject().put("model", "inworld/inworld-stt-1").put("language", "pt-BR"))
                .put("turn_detection", JSONObject().put("type", "semantic_vad").put("eagerness", "medium")
                    .put("create_response", currentImage == null).put("interrupt_response", true)))
                .put("output", JSONObject().put("model", "inworld-tts-2-flash").put("voice", config.getString("voice")).put("speed", 1)))
            put("providerData", JSONObject().put("tts", JSONObject().put("language", "pt-BR")))
            put("tools", JSONArray().put(JSONObject().put("type", "function").put("name", "get_local_context")
                .put("description", "Consulta clima e agenda reais do Android, somente com permissões já autorizadas. Use antes de responder sobre clima ou compromissos.")
                .put("parameters", JSONObject().put("type", "object").put("properties", JSONObject().put("question", JSONObject().put("type", "string")))
                    .put("required", JSONArray().put("question")))))
        }))
    }

    private fun handleEvent(raw: String) {
        val event = runCatching { JSONObject(raw) }.getOrNull() ?: return
        when (event.optString("type")) {
            "session.updated" -> { ready = true; onSessionChanged(true); pendingText?.let { pendingText = null; sendUserMessage(it) } }
            "input_audio_buffer.speech_started" -> { transcript.clear(); visionJob?.cancel() /* provider cancels its active audio response */ }
            "conversation.item.input_audio_transcription.completed" -> event.optString("transcript").takeIf(String::isNotBlank)?.let {
                onUserTranscript(it); currentImage?.let { image -> analyze(image, it) }
            }
            "response.function_call_arguments.done" -> if (event.optString("name") == "get_local_context") scope.launch {
                val callEpoch = epoch
                val callId = event.optString("call_id")
                val question = runCatching { JSONObject(event.optString("arguments")).optString("question") }.getOrDefault("").take(1000)
                val output = runCatching { FluxLocalContext.collect(context, app.api, question).context }.getOrElse { "Consulta indisponível; não invente dados." }
                if (active && callEpoch == epoch && callId.isNotBlank()) send(JSONObject().put("type", "conversation.item.create")
                    .put("item", JSONObject().put("type", "function_call_output").put("call_id", callId).put("output", output.ifBlank { "Dados não autorizados ou indisponíveis." })))
            }
            "response.output_audio_transcript.delta", "response.output_text.delta", "response.audio_transcript.delta" -> transcript.append(event.optString("delta"))
            "response.done" -> { if (transcript.isNotBlank()) onAgentResponse(transcript.toString()); transcript.clear() }
            "error" -> { onError(event.optJSONObject("error")?.optString("message") ?: "Erro na sessão de áudio.") }
        }
    }

    private fun send(value: JSONObject) {
        if (channel?.state() != DataChannel.State.OPEN) return
        channel?.send(DataChannel.Buffer(ByteBuffer.wrap(value.toString().toByteArray(Charsets.UTF_8)), false))
    }
    override fun sendUserMessage(message: String) {
        if (message.isBlank()) return
        if (!active) startSession()
        if (!ready) { pendingText = message; return }
        currentImage?.let { analyze(it, message); return }
        send(JSONObject().put("type", "conversation.item.create").put("item", JSONObject().put("type", "message").put("role", "user")
            .put("content", JSONArray().put(JSONObject().put("type", "input_text").put("text", message + "\n" + contextNote)))))
        contextNote = ""
        send(JSONObject().put("type", "response.create"))
    }
    override fun sendContextualUpdate(context: String) { contextNote = context.take(3000) }
    override fun sendScreenFrame(frame: Bitmap, prompt: String) {
        val ratio = minOf(1f, 1280f / maxOf(frame.width, frame.height))
        val scaled = Bitmap.createScaledBitmap(frame, (frame.width * ratio).toInt().coerceAtLeast(1), (frame.height * ratio).toInt().coerceAtLeast(1), true)
        val bytes = ByteArrayOutputStream().use { output -> scaled.compress(Bitmap.CompressFormat.JPEG, 82, output); output.toByteArray() }
        if (scaled !== frame) scaled.recycle()
        val image = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        currentImage = image
        if (ready) configureSession()
        analyze(image, prompt)
    }
    private fun analyze(image: String, prompt: String) {
        visionJob?.cancel()
        val analysisEpoch = epoch
        visionJob = scope.launch {
            try {
                val result = app.api.analyzeImage(image, prompt)
                if (!active || analysisEpoch != epoch) return@launch
                onAgentResponse(result)
                if (ready) send(JSONObject().put("type", "response.speak").put("text", result.take(1000)))
            } catch (failure: Exception) {
                if (failure !is CancellationException && active && analysisEpoch == epoch) onError(failure.message ?: "Não consegui analisar a captura.")
            }
        }
    }

    private fun observer(sessionEpoch: Int) = object : PeerConnection.Observer {
        override fun onSignalingChange(state: PeerConnection.SignalingState) = Unit
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
            if (state == PeerConnection.IceConnectionState.FAILED) scope.launch {
                if (active && sessionEpoch == epoch) { onError("O áudio perdeu a conexão. Feche e tente novamente."); endSession() }
            }
        }
        override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) = Unit
        override fun onIceCandidate(candidate: IceCandidate) = Unit
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) = Unit
        override fun onAddStream(stream: MediaStream) = Unit
        override fun onRemoveStream(stream: MediaStream) = Unit
        override fun onDataChannel(channel: DataChannel) = Unit
        override fun onRenegotiationNeeded() = Unit
        override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) = Unit
    }
    private suspend fun createOffer(connection: PeerConnection): SessionDescription = suspendCancellableCoroutine { continuation ->
        connection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) { if (continuation.isActive) continuation.resume(sdp) }
            override fun onCreateFailure(error: String) { if (continuation.isActive) continuation.resumeWithException(IllegalStateException(error)) }
            override fun onSetSuccess() = Unit
            override fun onSetFailure(error: String) = Unit
        }, MediaConstraints())
    }
    private suspend fun setDescription(connection: PeerConnection, sdp: SessionDescription, local: Boolean): Unit = suspendCancellableCoroutine { continuation ->
        val callback = object : SdpObserver {
            override fun onSetSuccess() { if (continuation.isActive) continuation.resume(Unit) }
            override fun onSetFailure(error: String) { if (continuation.isActive) continuation.resumeWithException(IllegalStateException(error)) }
            override fun onCreateSuccess(sdp: SessionDescription) = Unit
            override fun onCreateFailure(error: String) = Unit
        }
        if (local) connection.setLocalDescription(callback, sdp) else connection.setRemoteDescription(callback, sdp)
    }
    override fun endSession() {
        levelListener?.invoke(0f)
        active = false; ready = false; ++epoch
        connectionJob?.cancel(); visionJob?.cancel(); pendingText = null; currentImage = null; contextNote = ""
        channel?.unregisterObserver(); channel?.close(); channel?.dispose(); channel = null
        peer?.close(); peer?.dispose(); peer = null
        track?.dispose(); track = null; source?.dispose(); source = null
        factory?.dispose(); factory = null; audioModule?.release(); audioModule = null
        audioManager.mode = previousMode; audioManager.isSpeakerphoneOn = previousSpeaker
        onSessionChanged(false)
    }
    override fun destroy() { endSession(); scope.cancel() }
    companion object { private var initialized = false }
}
