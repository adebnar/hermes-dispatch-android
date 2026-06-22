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
    fun bridgeUrl(): String? = settings.bridgeUrl()
    fun bridgeToken(): String? = settings.bridgeToken()
    fun pushEndpoint(): String? = settings.pushEndpoint()

    /** Persist the bridge URL + token, then verify them against the bridge. */
    suspend fun pairAndConnect(bridgeUrl: String, token: String, profile: String?): Result<Unit> =
        runCatching {
            require(bridgeUrl.startsWith("https://") || bridgeUrl.startsWith("http://")) {
                "Bridge URL must start with http(s)://"
            }
            settings.setBridgeUrl(bridgeUrl)
            settings.setBridgeToken(token.ifBlank { null })
            settings.setActiveProfile(profile)
            api.authCheck()
        }.onFailure {
            // Don't leave a half-paired state on failure.
            settings.setBridgeUrl(null)
            settings.setBridgeToken(null)
        }

    /**
     * Update the connection of an already-paired app. Unlike [pairAndConnect],
     * a failed verify rolls back to the previous URL/token so a typo doesn't
     * sign the user out. A blank [token] keeps the existing one.
     */
    suspend fun updateConnection(bridgeUrl: String, token: String): Result<Unit> {
        val prevUrl = settings.bridgeUrl()
        val prevToken = settings.bridgeToken()
        return runCatching {
            require(bridgeUrl.startsWith("https://") || bridgeUrl.startsWith("http://")) {
                "Bridge URL must start with http(s)://"
            }
            settings.setBridgeUrl(bridgeUrl.trimEnd('/'))
            settings.setBridgeToken(token.ifBlank { prevToken })
            api.authCheck()
        }.onFailure {
            settings.setBridgeUrl(prevUrl)
            settings.setBridgeToken(prevToken)
        }
    }

    fun setProfile(profile: String?) = settings.setActiveProfile(profile)

    /** Profile names available on the bridge, or empty on failure. */
    suspend fun availableProfiles(): List<String> =
        runCatching { api.profiles().profiles.map { it.name } }.getOrDefault(emptyList())

    suspend fun models() = runCatching { api.models() }.getOrNull()

    suspend fun pushInfo() = runCatching { api.pushInfo() }.getOrNull()

    suspend fun setModel(provider: String, model: String): Result<Unit> =
        runCatching { api.setModel(provider, model) }

    fun signOut() = settings.clear()
}
