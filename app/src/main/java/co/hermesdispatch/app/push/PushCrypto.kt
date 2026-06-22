package co.hermesdispatch.app.push

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-end decryption of push payloads. The bridge encrypts the push body with
 * AES-256-GCM when this device has registered a key, so the push relay (ntfy)
 * only ever sees ciphertext. Wire format produced by the bridge:
 *
 *     HDX1.<base64url(nonce)>.<base64url(ciphertext||tag)>
 */
object PushCrypto {
    private const val PREFIX = "HDX1."
    private const val URL_FLAGS = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP

    /** Generate a fresh 32-byte key, base64 (standard, no-wrap) encoded. */
    fun generateKeyBase64(): String {
        val raw = ByteArray(32)
        SecureRandom().nextBytes(raw)
        return Base64.encodeToString(raw, Base64.NO_WRAP)
    }

    fun isEncrypted(body: String): Boolean = body.startsWith(PREFIX)

    /** Decrypt an `HDX1.` body, or null if it isn't encrypted / can't be decrypted. */
    fun decrypt(body: String, keyBase64: String?): ByteArray? {
        if (!isEncrypted(body) || keyBase64.isNullOrBlank()) return null
        val parts = body.removePrefix(PREFIX).split(".")
        if (parts.size != 2) return null
        return try {
            val key = Base64.decode(keyBase64, Base64.NO_WRAP)
            val nonce = Base64.decode(parts[0], URL_FLAGS)
            val ct = Base64.decode(parts[1], URL_FLAGS)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
            cipher.doFinal(ct)
        } catch (_: Exception) {
            null
        }
    }
}
