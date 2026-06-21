package co.hermesdispatch.app.push

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Progress payload the bridge POSTs to the device's push topic. Kept small and
 * provider-agnostic so the same shape works over UnifiedPush/ntfy or FCM.
 */
@Serializable
data class PushMessage(
    val sessionId: String? = null,
    val title: String = "Hermes",
    val status: String = "",
    val done: Boolean = false,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        /** Parse a raw push body; falls back to treating it as a plain status line. */
        fun parse(bytes: ByteArray): PushMessage {
            val text = bytes.toString(Charsets.UTF_8).trim()
            return runCatching { json.decodeFromString<PushMessage>(text) }
                .getOrElse { PushMessage(status = text) }
        }
    }
}
