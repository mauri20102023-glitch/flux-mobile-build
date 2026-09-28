package ai.flux.mobile.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat

data class BluetoothAudioSnapshot(
    val name: String?,
    val hasInput: Boolean,
    val hasOutput: Boolean,
    val routeSelected: Boolean,
)

class BluetoothAudioRouter(private val context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)

    fun detectAndSelect(): BluetoothAudioSnapshot {
        if (Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) return BluetoothAudioSnapshot(null, false, false, false)

        val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).filter(::isBluetoothAudio)
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).filter(::isBluetoothAudio)
        val preferred = (inputs + outputs).distinctBy { it.id }.firstOrNull()
        val selected = if (Build.VERSION.SDK_INT >= 31 && preferred != null) {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.setCommunicationDevice(preferred)
        } else {
            @Suppress("DEPRECATION")
            if (preferred != null) {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.startBluetoothSco()
                true
            } else false
        }
        return BluetoothAudioSnapshot(
            name = preferred?.productName?.toString(),
            hasInput = inputs.isNotEmpty(),
            hasOutput = outputs.isNotEmpty(),
            routeSelected = selected,
        )
    }

    fun release() {
        if (Build.VERSION.SDK_INT >= 31) audioManager.clearCommunicationDevice()
        else @Suppress("DEPRECATION") audioManager.stopBluetoothSco()
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    private fun isBluetoothAudio(device: AudioDeviceInfo): Boolean = device.type in setOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
    )
}
