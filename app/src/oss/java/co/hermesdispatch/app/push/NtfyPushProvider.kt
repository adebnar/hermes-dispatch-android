package co.hermesdispatch.app.push

import android.content.Context
import co.hermesdispatch.app.data.prefs.SecureSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.unifiedpush.android.connector.UnifiedPush

/**
 * Default (OSS) push provider: UnifiedPush via the user's distributor (e.g. the
 * ntfy app). On [register] it picks a saved distributor (or the first available
 * one) and registers; the resulting endpoint arrives asynchronously in
 * [UnifiedPushReceiver.onNewEndpoint] and is persisted there. No Google libraries.
 */
@Singleton
class NtfyPushProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SecureSettings,
) : PushProvider {
    override val id: String = "ntfy"

    override suspend fun register(): String? {
        NotificationHelper.ensureChannels(context)
        if (UnifiedPush.getAckDistributor(context) == null) {
            val chosen = UnifiedPush.getDistributors(context).firstOrNull()
                ?: return null // no distributor installed (e.g. ntfy app missing)
            UnifiedPush.saveDistributor(context, chosen)
        }
        UnifiedPush.register(context)
        return settings.pushEndpoint()
    }

    override suspend fun unregister() {
        UnifiedPush.unregister(context)
        settings.setPushEndpoint(null)
    }
}
