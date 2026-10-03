package ai.flux.mobile.assistant

import android.content.Intent
import android.speech.RecognitionService
import android.speech.SpeechRecognizer

/**
 * Required by Android's assistant service metadata. On Android 12+, this
 * referenced recognition service is not used by the platform. FLUX's own
 * wake-word listener uses the device's on-device recognizer directly.
 * Earlier Android versions do not support that FLUX wake-word path.
 */
class FluxRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent, listener: Callback) {
        listener.error(SpeechRecognizer.ERROR_CLIENT)
    }

    override fun onStopListening(listener: Callback) = Unit

    override fun onCancel(listener: Callback) = Unit
}
