package ai.flux.mobile.assistant

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionService
import java.lang.ref.WeakReference

class FluxVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        current = WeakReference(this)
    }

    override fun onShutdown() {
        if (current?.get() === this) current = null
        super.onShutdown()
    }

    private fun showFlux(vision: Boolean) {
        val args = Bundle().apply { putBoolean(ARG_VISION, vision) }
        val flags = VoiceInteractionSession.SHOW_WITH_ASSIST or
            if (vision) VoiceInteractionSession.SHOW_WITH_SCREENSHOT else 0
        showSession(args, flags)
    }

    companion object {
        const val ARG_VISION = "flux_vision_requested"
        private var current: WeakReference<FluxVoiceInteractionService>? = null

        fun requestSession(vision: Boolean): Boolean {
            val service = current?.get() ?: return false
            service.mainExecutor.execute { service.showFlux(vision) }
            return true
        }
    }
}
