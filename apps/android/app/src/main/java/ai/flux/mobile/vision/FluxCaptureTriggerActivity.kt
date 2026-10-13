package ai.flux.mobile.vision

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** Opening an activity closes the notification shade before selecting a frame. */
class FluxCaptureTriggerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this, FluxScreenCaptureService::class.java).setAction(FluxScreenCaptureService.CAPTURE))
        finish()
    }
}
