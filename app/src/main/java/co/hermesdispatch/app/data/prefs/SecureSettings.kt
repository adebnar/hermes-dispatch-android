package co.hermesdispatch.app.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Credential + connection settings, stored in [EncryptedSharedPreferences]
 * (AES-256, keyed by an Android Keystore master key). The bridge URL, session
 * cookie/token, and push endpoint never touch plaintext on disk.
 */
@Singleton
class SecureSettings @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "hermes_dispatch_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun bridgeUrl(): String? = prefs.getString(KEY_BRIDGE_URL, null)
    fun bridgeToken(): String? = prefs.getString(KEY_BRIDGE_TOKEN, null)
    fun activeProfile(): String? = prefs.getString(KEY_PROFILE, null)
    fun pushEndpoint(): String? = prefs.getString(KEY_PUSH_ENDPOINT, null)
    fun isPaired(): Boolean = !bridgeUrl().isNullOrBlank()

    /** Prefer the bridge's server-side transcription over on-device STT. */
    fun serverTranscription(): Boolean = prefs.getBoolean(KEY_SERVER_STT, false)

    /** Base64 AES-256 key used to E2EE-encrypt push payloads, or null if off. */
    fun pushKey(): String? = prefs.getString(KEY_PUSH_KEY, null)

    fun setBridgeUrl(url: String?) = prefs.edit().putString(KEY_BRIDGE_URL, url).apply()
    fun setBridgeToken(token: String?) = prefs.edit().putString(KEY_BRIDGE_TOKEN, token).apply()
    fun setActiveProfile(profile: String?) = prefs.edit().putString(KEY_PROFILE, profile).apply()
    fun setPushEndpoint(endpoint: String?) = prefs.edit().putString(KEY_PUSH_ENDPOINT, endpoint).apply()
    fun setServerTranscription(on: Boolean) = prefs.edit().putBoolean(KEY_SERVER_STT, on).apply()
    fun setPushKey(key: String?) = prefs.edit().putString(KEY_PUSH_KEY, key).apply()

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_BRIDGE_URL = "bridge_url"
        const val KEY_BRIDGE_TOKEN = "bridge_token"
        const val KEY_PROFILE = "active_profile"
        const val KEY_PUSH_ENDPOINT = "push_endpoint"
        const val KEY_SERVER_STT = "server_transcription"
        const val KEY_PUSH_KEY = "push_key"
    }
}
