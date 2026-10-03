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

    fun coreUrl(): String = preferences.getString("core_url", "").orEmpty().trimEnd('/')
        .ifBlank { BuildConfig.FLUX_CORE_URL.trimEnd('/') }

    fun updateCoreUrl(value: String) {
        val normalized = value.trim().trimEnd('/')
        val uri = runCatching { URI(normalized) }.getOrNull()
        require(uri != null && !uri.host.isNullOrBlank() && uri.rawUserInfo == null &&
            uri.rawQuery == null && uri.rawFragment == null && uri.path.orEmpty().isEmpty() &&
            (uri.scheme == "https" || isPrivateLocalHttp(normalized))) {
            "Use HTTPS ou um endereço local privado do Flux Core"
        }
        // A credential issued by one Core must never be sent to another host.
        if (normalized != coreUrl()) clearAuthToken()
        preferences.edit().putString("core_url", normalized).apply()
    }

    private fun isPrivateLocalHttp(value: String): Boolean = runCatching {
        val uri = URI(value)
        if (uri.scheme != "http") return@runCatching false
        val host = uri.host?.lowercase() ?: return@runCatching false
        if (host == "localhost") return@runCatching true
        val octets = host.split('.').map { it.toIntOrNull() ?: return@runCatching false }
        if (octets.size != 4 || octets.any { it !in 0..255 }) return@runCatching false
        octets[0] == 127 && octets[1] == 0 && octets[2] == 0 && octets[3] == 1 ||
            octets[0] == 10 ||
            octets[0] == 192 && octets[1] == 168 ||
            octets[0] == 172 && octets[1] in 16..31
    }.getOrDefault(false)

    fun authToken(): String = secureTokenStore.read()

    fun authTokenConfigured(): Boolean = authToken().isNotBlank()

    fun updateAuthToken(value: String) {
        secureTokenStore.write(value)
    }

    fun clearAuthToken() = secureTokenStore.write("")

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
