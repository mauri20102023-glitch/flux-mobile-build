package ai.flux.mobile.vision

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.Base64
import ai.flux.mobile.FluxApplication
import ai.flux.mobile.MainActivity
import ai.flux.mobile.R
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

/** One consent, one frame, one analysis. No recording or capture persisted to disk. */
class FluxScreenCaptureService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var analyzing = false
    private val timeout = Runnable { finish("A captura expirou. Autorize uma nova captura quando precisar.") }
    private val callback = object : MediaProjection.Callback() {
        override fun onStop() {
            if (projection != null) {
                releaseCapture()
                if (!analyzing) finish("O compartilhamento da tela foi encerrado pelo Android.")
            }
        }
    }

    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            STOP -> { stopSelf(); return START_NOT_STICKY }
            CAPTURE -> { capture(); return START_NOT_STICKY }
            START -> {
                if (projection != null || analyzing) return START_NOT_STICKY
                try {
                    val manager = getSystemService(NotificationManager::class.java)
                    manager.createNotificationChannel(NotificationChannel(CHANNEL, "FLUX Vision", NotificationManager.IMPORTANCE_DEFAULT))
                    val notification = notification("Abra o conteúdo desejado e toque em Analisar agora.", true)
                    if (Build.VERSION.SDK_INT >= 29) startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
                    else startForeground(ID, notification)
                    val consent = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra("consent", Intent::class.java)
                        else @Suppress("DEPRECATION") intent.getParcelableExtra<Intent>("consent")
                    requireNotNull(consent) { "A autorização da captura não foi recebida." }
                    projection = getSystemService(MediaProjectionManager::class.java).getMediaProjection(Activity.RESULT_OK, consent)
                    projection!!.registerCallback(callback, handler)
                    val metrics = resources.displayMetrics
                    reader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
                    display = projection!!.createVirtualDisplay("FLUX-Vision", metrics.widthPixels, metrics.heightPixels,
                        metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader!!.surface, null, handler)
                    // Drain frames, keeping no history. The action captures the next fresh frame.
                    reader!!.setOnImageAvailableListener({ source -> source.acquireLatestImage()?.close() }, handler)
                    handler.postDelayed(timeout, 60000)
                } catch (failure: Exception) { finish(failure.message ?: "Não consegui iniciar a captura autorizada.") }
            }
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun capture() {
        if (analyzing) return
        if(reader==null){stopSelf();return}
        analyzing = true
        getSystemService(NotificationManager::class.java).notify(ID, notification("Capturando um quadro e analisando…", false))
        // Let the notification shade close before selecting the next real frame.
        handler.postDelayed({
            reader?.setOnImageAvailableListener({ source ->
                var image = source.acquireLatestImage()
                if (image == null) return@setOnImageAvailableListener
                source.setOnImageAvailableListener(null, null)
                try {
                    val frame = image!!
                    val plane = frame.planes[0]
                    val paddedWidth = plane.rowStride / plane.pixelStride
                    val padded = Bitmap.createBitmap(paddedWidth, frame.height, Bitmap.Config.ARGB_8888)
                    padded.copyPixelsFromBuffer(plane.buffer)
                    val crop = Bitmap.createBitmap(padded, 0, 0, frame.width, frame.height)
                    val ratio = minOf(1f, 1280f / maxOf(crop.width, crop.height))
                    val scaled = Bitmap.createScaledBitmap(crop, (crop.width * ratio).toInt().coerceAtLeast(1), (crop.height * ratio).toInt().coerceAtLeast(1), true)
                    val bytes = ByteArrayOutputStream().use { output ->
                        scaled.compress(Bitmap.CompressFormat.JPEG, 82, output); output.toByteArray()
                    }
                    if (scaled !== crop) scaled.recycle()
                    if (crop !== padded) crop.recycle()
                    padded.recycle()
                    frame.close(); image = null
                    releaseCapture()
                    scope.launch {
                        try {
                            val data = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                            val result = (application as FluxApplication).api.analyzeImage(data,
                                "Explique o conteúdo visível desta captura. Leia os textos relevantes e proponha ações úteis. Se a captura estiver vazia, protegida ou ilegível, avise; não invente conteúdo.")
                            finish(result)
                        } catch (failure: Exception) {
                            if (failure !is CancellationException) finish(failure.message ?: "A análise da tela falhou.")
                        }
                    }
                } catch (failure: Exception) { finish(failure.message ?: "Não consegui ler o quadro capturado.") }
                finally { image?.close() }
            }, handler)
        }, 800)
    }

    private fun notification(message: String, captureAvailable: Boolean): Notification {
        val open = PendingIntent.getActivity(this, 10, Intent(this, MainActivity::class.java).putExtra("vision_result", true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_flux_notification)
            .setContentTitle("FLUX Vision").setContentText(message).setContentIntent(open).setOngoing(true)
        if (captureAvailable) builder.addAction(Notification.Action.Builder(null, "Analisar agora",
            PendingIntent.getActivity(this, 11, Intent(this, FluxCaptureTriggerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)).build())
        builder.addAction(Notification.Action.Builder(null, "Encerrar", action(STOP, 12)).build())
        return builder.build()
    }
    private fun action(name: String, request: Int) = PendingIntent.getService(this, request,
        Intent(this, FluxScreenCaptureService::class.java).setAction(name), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun finish(result: String) {
        // Content stays in process memory, never in notification text, intent extras or logs.
        pendingResult = result.take(16000)
        stopForeground(STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java).notify(RESULT_ID,
            Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_flux_notification).setContentTitle("FLUX Vision")
                .setContentText("Análise disponível. Toque para abrir no FLUX.").setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(this, 10,
                    Intent(this, MainActivity::class.java).putExtra("vision_result", true),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)).build())
        stopSelf()
    }
    private fun releaseCapture() {
        handler.removeCallbacks(timeout)
        display?.release(); display = null
        reader?.close(); reader = null
        val old = projection; projection = null
        old?.unregisterCallback(callback); old?.stop()
    }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); releaseCapture(); scope.cancel(); super.onDestroy() }
    companion object {
        const val START = "ai.flux.vision.START"
        const val CAPTURE = "ai.flux.vision.CAPTURE"
        private const val STOP = "ai.flux.vision.STOP"
        private const val CHANNEL = "flux_vision"
        private const val ID = 4201
        private const val RESULT_ID = 4202
        private var pendingResult: String? = null
        fun consumeResult(): String? = pendingResult.also { pendingResult = null }
    }
}
