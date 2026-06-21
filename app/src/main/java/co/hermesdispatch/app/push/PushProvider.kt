package co.hermesdispatch.app.push

/**
 * Background-push abstraction. The app screens never depend on a concrete
 * provider — the common (`oss`) path uses UnifiedPush/ntfy and is Google-library
 * free; the `play` flavor can additionally bind a runtime-configured FCM
 * implementation (see docs/API-CONTRACT.md and the plan's "Admin → Notifications").
 */
interface PushProvider {
    /** Stable identifier shown in the Admin → Notifications screen. */
    val id: String

    /** Begin receiving pushes; returns the endpoint to register with the bridge
     *  (e.g. an ntfy topic URL), or null if the provider self-registers. */
    suspend fun register(): String?

    /** Stop receiving pushes and release any subscription. */
    suspend fun unregister()
}
