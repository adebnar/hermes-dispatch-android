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
        SecureSettings(context).setPushEndpoint(endpoint.url)
        // TODO(Phase 4+): POST the endpoint to the bridge so it can target this device.
    }

    override fun onRegistrationFailed(context: Context, reason: FailedReason, instance: String) {
        // Surfaced in Admin → Notifications in a later iteration.
    }

    override fun onUnregistered(context: Context, instance: String) {
        SecureSettings(context).setPushEndpoint(null)
    }
}
