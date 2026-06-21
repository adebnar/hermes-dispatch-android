package co.hermesdispatch.app.push

import co.hermesdispatch.app.data.prefs.SecureSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `play`-flavor push provider: bring-your-own Firebase, configured at RUNTIME
 * (no google-services.json, no rebuild). The user pastes their Firebase project
 * values in Admin → Notifications; Phase 4 will initialize a named FirebaseApp via
 * FirebaseOptions.Builder and obtain an FCM token here.
 *
 * Kept dormant by default so the common path stays dead-simple — the OSS flavor
 * does not include this class or any Firebase dependency.
 */
@Singleton
class FcmPushProvider @Inject constructor(
    private val settings: SecureSettings,
) : PushProvider {
    override val id: String = "fcm"

    override suspend fun register(): String? {
        // Phase 4: FirebaseApp.initializeApp(ctx, options, "hermes") then
        // FirebaseMessaging.getInstance(app).token -> register with bridge.
        return settings.pushEndpoint()
    }

    override suspend fun unregister() = settings.setPushEndpoint(null)
}
