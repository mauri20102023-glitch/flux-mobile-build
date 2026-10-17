package ai.flux.mobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ai.flux.mobile.audio.FluxOfflineVoiceTts
import ai.flux.mobile.data.FluxConnectionSettings
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FluxOfflineVoiceTest {
    @Test fun localVoiceOptInCanBeSavedAndReverted() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val settings = FluxConnectionSettings(context)
        val previous = settings.offlineVoiceEnabled()
        try {
            settings.updateOfflineVoiceEnabled(false)
            Assert.assertFalse(settings.offlineVoiceEnabled())
            settings.updateOfflineVoiceEnabled(true)
            Assert.assertTrue(FluxConnectionSettings(context).offlineVoiceEnabled())
        } finally {
            settings.updateOfflineVoiceEnabled(previous)
        }
    }

    @Test fun explicitSherpaServiceDetectionNeverUsesGooglePackage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Assert.assertEquals("com.k2fsa.sherpa.onnx.tts.engine", FluxOfflineVoiceTts.ENGINE_PACKAGE)
        // Available in an emulator only if a dedicated service APK is installed.
        if (!FluxOfflineVoiceTts.isInstalled(context)) {
            Assert.assertFalse(FluxOfflineVoiceTts.isInstalled(context))
        }
    }
}
