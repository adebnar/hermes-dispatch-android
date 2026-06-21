package co.hermesdispatch.app.data.remote

import co.hermesdispatch.app.data.prefs.SecureSettings
import co.hermesdispatch.app.data.remote.dto.ChatStartRequest
import co.hermesdispatch.app.data.remote.dto.ChatStartResponse
import co.hermesdispatch.app.data.remote.dto.CronDto
import co.hermesdispatch.app.data.remote.dto.LoginRequest
import co.hermesdispatch.app.data.remote.dto.LoginResponse
import co.hermesdispatch.app.data.remote.dto.McpServerDto
import co.hermesdispatch.app.data.remote.dto.SessionDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin typed wrapper over the Hermes (webui/bridge) HTTP contract. The base URL
 * and active profile are read live from [SecureSettings] so re-pairing or
 * switching profiles takes effect without rebuilding the client.
 */
@Singleton
class HermesApi @Inject constructor(
    private val client: HttpClient,
    private val settings: SecureSettings,
    private val sse: SseClient,
) {
    class NotPairedException : IllegalStateException("No Hermes bridge configured")

    private fun base(): String =
        settings.bridgeUrl()?.trimEnd('/') ?: throw NotPairedException()

    private fun io.ktor.client.request.HttpRequestBuilder.profile() {
        settings.activeProfile()?.let { header("X-Hermes-Profile", it) }
    }

    suspend fun login(password: String): Boolean =
        client.post("${base()}/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(password))
        }.body<LoginResponse>().ok

    suspend fun sessions(): List<SessionDto> =
        client.get("${base()}/api/sessions") { profile() }.body()

    suspend fun crons(): List<CronDto> =
        client.get("${base()}/api/crons") { profile() }.body()

    suspend fun mcpServers(): List<McpServerDto> =
        client.get("${base()}/api/mcp/servers") { profile() }.body()

    suspend fun startChat(req: ChatStartRequest): String =
        client.post("${base()}/api/chat/start") {
            contentType(ContentType.Application.Json)
            profile()
            setBody(req)
        }.body<ChatStartResponse>().streamId

    /** Server-Sent Events for an in-flight run. */
    fun streamChat(streamId: String) =
        sse.stream("${base()}/api/chat/stream?stream_id=$streamId") { profile() }

    suspend fun pauseCron(id: String) = cronAction("pause", id)
    suspend fun resumeCron(id: String) = cronAction("resume", id)
    suspend fun runCron(id: String) = cronAction("run", id)
    suspend fun deleteCron(id: String) = cronAction("delete", id)

    private suspend fun cronAction(action: String, id: String) {
        client.post("${base()}/api/crons/$action") {
            contentType(ContentType.Application.Json)
            profile()
            setBody(mapOf("id" to id))
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }
    }
}
