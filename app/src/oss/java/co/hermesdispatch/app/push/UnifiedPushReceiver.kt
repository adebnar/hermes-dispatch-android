package co.hermesdispatch.app.push

import android.content.Context
import co.hermesdispatch.app.data.prefs.SecureSettings
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.MessagingReceiver
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage as UpPushMessage

/**
 * Receives UnifiedPush (e.g. ntfy) callbacks (connector 3.x). Incoming messages
 * become lock-screen progress / completion notifications; the endpoint is
 * persisted so the user can register it with their bridge.
 *
 * Manifest-declared [android.content.BroadcastReceiver], so no Hilt injection —
 * it constructs [SecureSettings] (a thin EncryptedSharedPreferences wrapper).
 */
class UnifiedPushReceiver : MessagingReceiver() {

    override fun onMessage(context: Context, message: UpPushMessage, instance: String) {
        NotificationHelper.show(context, PushMessage.parse(message.content))
    }

    override fun onNewEndpoint(context: Context, endpoint: PushEndpoint, instance: String) {
        val settings = SecureSettings(context)
        settings.setPushEndpoint(endpoint.url)
        // Register this device's endpoint with the bridge (off the main thread).
        val pending = goAsync()
        Thread {
            try {
                BridgeRegistrar.register(settings.bridgeUrl(), settings.bridgeToken(), endpoint.url)
            } finally {
                pending.finish()
            }
        }.start()
    }

    override fun onRegistrationFailed(context: Context, reason: FailedReason, instance: String) {
        // Surfaced in Admin → Notifications in a later iteration.
    }

    override fun onUnregistered(context: Context, instance: String) {
        val settings = SecureSettings(context)
        val endpoint = settings.pushEndpoint()
        settings.setPushEndpoint(null)
        if (endpoint != null) {
            val pending = goAsync()
            Thread {
                try {
                    BridgeRegistrar.unregister(settings.bridgeUrl(), settings.bridgeToken(), endpoint)
                } finally {
                    pending.finish()
                }
            }.start()
        }
    }
}
