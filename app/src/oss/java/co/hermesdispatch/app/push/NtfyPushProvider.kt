package co.hermesdispatch.app.push

import co.hermesdispatch.app.data.prefs.SecureSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default (OSS) push provider: UnifiedPush via ntfy. The user supplies an ntfy
 * topic endpoint during pairing; the bridge POSTs progress lines to it and the
 * ntfy distributor app delivers them to this device. No Google libraries.
 *
 * NOTE: Phase 4 wires the real UnifiedPush registration + receiver. For now this
 * persists/returns the configured endpoint so the rest of the app is complete.
 */
@Singleton
class NtfyPushProvider @Inject constructor(
    private val settings: SecureSettings,
) : PushProvider {
    override val id: String = "ntfy"

    override suspend fun register(): String? = settings.pushEndpoint()

    override suspend fun unregister() = settings.setPushEndpoint(null)
}
