package co.hermesdispatch.app.data.repository

import co.hermesdispatch.app.data.prefs.SecureSettings
import co.hermesdispatch.app.data.remote.HermesApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: HermesApi,
    private val settings: SecureSettings,
) {
    fun isPaired(): Boolean = settings.isPaired()
    fun activeProfile(): String? = settings.activeProfile()

    /** Persist the bridge URL, then authenticate. The session cookie is stored
     *  by the HTTP client's cookie jar on success. */
    suspend fun pairAndLogin(bridgeUrl: String, password: String, profile: String?): Result<Unit> =
        runCatching {
            require(bridgeUrl.startsWith("https://") || bridgeUrl.startsWith("http://")) {
                "Bridge URL must start with http(s)://"
            }
            settings.setBridgeUrl(bridgeUrl)
            settings.setActiveProfile(profile)
            val ok = api.login(password)
            check(ok) { "Login rejected by the bridge" }
        }.onFailure {
            // Don't leave a half-paired state on failure.
            settings.setBridgeUrl(null)
        }

    fun setProfile(profile: String?) = settings.setActiveProfile(profile)

    fun signOut() = settings.clear()
}
