package ai.flux.mobile.data

import android.content.Context
import ai.flux.mobile.BuildConfig
import java.net.URI
import java.util.UUID

class FluxConnectionSettings(context: Context) {
    private val preferences = context.getSharedPreferences("flux_connection", Context.MODE_PRIVATE)
    private val secureTokenStore = FluxSecureTokenStore(context)
    private val secureGeminiKeyStore = FluxSecureTokenStore(
        context = context,
        keyAlias = "flux_gemini_api_key_v1",
        ivPreference = "gemini_api_key_iv",
        valuePreference = "gemini_api_key_value",
    )

    fun coreUrl(): String = BuildConfig.FLUX_CORE_URL.trimEnd('/').ifBlank {
        preferences.getString("core_url", "").orEmpty().trimEnd('/')
    }

    fun updateCoreUrl(value: String) {
        if (BuildConfig.FLUX_CORE_URL.isNotBlank()) {
            preferences.edit().remove("core_url").apply()
            return
        }
        val normalized = value.trim().trimEnd('/')
        require(normalized.startsWith("https://") || isPrivateLocalHttp(normalized)) {
            "Use HTTPS ou um endereço local privado do Flux Core"
        }
        preferences.edit().putString("core_url", normalized).apply()
    }

    private fun isPrivateLocalHttp(value: String): Boolean = runCatching {
        val uri = URI(value)
        if (uri.scheme != "http") return@runCatching false
        val host = uri.host?.lowercase() ?: return@runCatching false
        host == "localhost" || host == "127.0.0.1" || host.startsWith("10.") ||
            host.startsWith("192.168.") || Regex("^172\\.(1[6-9]|2\\d|3[01])\\.").containsMatchIn(host)
    }.getOrDefault(false)

    fun authToken(): String = secureTokenStore.read()

    fun authTokenConfigured(): Boolean = authToken().isNotBlank()

    fun updateAuthToken(value: String) {
        secureTokenStore.write(value)
    }

    fun clearAuthToken() = secureTokenStore.write("")

    fun pairingPrivateKey(): String = BuildConfig.FLUX_PAIRING_PRIVATE_KEY.trim()

    fun geminiApiKey(): String = secureGeminiKeyStore.read()

    fun geminiApiKeyConfigured(): Boolean = geminiApiKey().isNotBlank()

    fun updateGeminiApiKey(value: String) {
        val clean = value.trim()
        if (clean.isNotEmpty()) {
            require(clean.length >= 30 && !clean.any(Char::isWhitespace)) {
                "A chave do Gemini parece incompleta."
            }
        }
        secureGeminiKeyStore.write(clean)
    }

    fun clearGeminiApiKey() = secureGeminiKeyStore.write("")

    fun deviceId(): String {
        preferences.getString("device_id", null)?.takeIf(String::isNotBlank)?.let { return it }
        val generated = "mobile-${UUID.randomUUID()}"
        preferences.edit().putString("device_id", generated).apply()
        return generated
    }
}
