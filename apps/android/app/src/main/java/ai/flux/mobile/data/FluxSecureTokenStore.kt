package ai.flux.mobile.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Keeps a secret encrypted by a non-exportable Android Keystore key. */
class FluxSecureTokenStore(
    context: Context,
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
    private val ivPreference: String = DEFAULT_KEY_IV,
    private val valuePreference: String = DEFAULT_KEY_VALUE,
) {
    private val preferences = context.getSharedPreferences("flux_secure_connection", Context.MODE_PRIVATE)

    fun read(): String {
        val encrypted = preferences.getString(valuePreference, null) ?: return ""
        val iv = preferences.getString(ivPreference, null) ?: return ""
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)),
            )
            String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrElse {
            clear()
            ""
        }
    }

    fun write(value: String) {
        val clean = value.trim()
        if (clean.isEmpty()) {
            clear()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(clean.toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString(ivPreference, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(valuePreference, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    private fun clear() {
        preferences.edit().remove(ivPreference).remove(valuePreference).apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val DEFAULT_KEY_ALIAS = "flux_core_access_token_v1"
        const val DEFAULT_KEY_IV = "core_token_iv"
        const val DEFAULT_KEY_VALUE = "core_token_value"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
