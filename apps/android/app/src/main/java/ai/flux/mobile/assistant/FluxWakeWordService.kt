package ai.flux.mobile.assistant

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import ai.flux.mobile.MainActivity
import ai.flux.mobile.R
import java.text.Normalizer

/**
 * Optional, visible wake-word listener. It deliberately uses only Android's
 * on-device recognizer: audio is never sent to an unknown cloud fallback.
 */
class FluxWakeWordService : Service(), RecognitionListener {
    private var recognizer: SpeechRecognizer? = null
    private var paused = false
    private var restarting = false
    private var destroyed = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        active = this
        createChannel()
        val notification = notification("Diga “Flux” para chamar")
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        startRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!destroyed && !paused) startRecognizer()
        return START_STICKY
    }

    override fun onDestroy() {
        destroyed=true
        handler.removeCallbacksAndMessages(null)
        if (active === this) active = null
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startRecognizer() {
        if (destroyed || paused || restarting || recognizer != null) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }
        if (Build.VERSION.SDK_INT < 31 || !SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            updateNotification("Ativação por voz indisponível neste aparelho")
            return
        }
        runCatching {
            recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this).also {
                it.setRecognitionListener(this)
                it.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                })
            }
            updateNotification("Diga “Flux” para chamar")
        }.onFailure {
            recognizer?.destroy()
            recognizer = null
            scheduleRestart()
        }
    }

    private fun consume(bundle: Bundle?) {
        val candidates = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
        val match = candidates.firstOrNull { WAKE_WORD.containsMatchIn(normalize(it)) } ?: return
        val normalized = normalize(match)
        val vision = normalized.contains("tela") && listOf("veja", "olhe", "analise", "leia").any(normalized::contains)
        pauseRecognition()
        if (!FluxVoiceInteractionService.requestSession(vision)) {
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("start_voice", !vision)
                putExtra("start_vision", vision)
            })
        }
    }

    private fun normalize(value: String): String = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")

    private fun pauseRecognition() {
        paused = true
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        updateNotification("FLUX em conversa")
    }

    private fun resumeRecognition() {
        if (!paused) return
        paused = false
        scheduleRestart(450L)
    }

    private fun scheduleRestart(delay: Long = 900L) {
        if (destroyed || paused || restarting) return
        restarting = true
        mainExecutor.execute {
            handler.postDelayed({
                restarting = false
                if (!destroyed && !paused) startRecognizer()
            }, delay)
        }
    }

    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
    override fun onPartialResults(partialResults: Bundle?) = consume(partialResults)
    override fun onResults(results: Bundle?) {
        consume(results)
        if (!paused) {
            recognizer?.destroy()
            recognizer = null
            scheduleRestart(250L)
        }
    }

    override fun onError(error: Int) {
        recognizer?.destroy()
        recognizer = null
        if (!paused) scheduleRestart(if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1_500L else 600L)
    }

    private fun notification(detail: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, FluxWakeWordService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_flux_notification)
            .setContentTitle("FLUX por voz ativo")
            .setContentText(detail)
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, "Desativar", stop)
            .build()
    }

    private fun updateNotification(detail: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(detail))
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Ativação por voz FLUX", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    companion object {
        private val WAKE_WORD = Regex("\\bflux\\b")
        private const val CHANNEL_ID = "flux_wake_word"
        private const val NOTIFICATION_ID = 2106
        private const val ACTION_STOP = "ai.flux.mobile.STOP_WAKE_WORD"
        @Volatile private var active: FluxWakeWordService? = null

        fun pauseForConversation() = active?.pauseRecognition()
        fun resumeAfterConversation() = active?.resumeRecognition()
    }
}
