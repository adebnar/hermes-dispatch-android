package co.hermesdispatch.app.data.remote

import co.hermesdispatch.app.data.prefs.SecureSettings
import co.hermesdispatch.app.data.remote.dto.McpServerDto
import co.hermesdispatch.app.data.remote.dto.ProfilesResponse
import co.hermesdispatch.app.data.remote.dto.ScheduleDto
import co.hermesdispatch.app.data.remote.dto.StartTaskRequest
import co.hermesdispatch.app.data.remote.dto.StartTaskResponse
import co.hermesdispatch.app.data.remote.dto.SteerRequest
import co.hermesdispatch.app.data.remote.dto.TaskDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client for the Hermes Dispatch **bridge** `/v1` API. The bridge fronts the
 * user's Hermes and holds long-running tasks; we authenticate with a single
 * bearer token (the bridge holds the upstream webui password server-side).
 *
 * Base URL and token are read live from [SecureSettings] so re-pairing takes
 * effect without rebuilding the client.
 */
@Singleton
class HermesApi @Inject constructor(
    private val client: HttpClient,
    private val settings: SecureSettings,
    private val sse: SseClient,
) {
    class NotPairedException : IllegalStateException("No bridge configured")

    private fun base(): String =
        settings.bridgeUrl()?.trimEnd('/') ?: throw NotPairedException()

    private fun HttpRequestBuilder.auth() {
        settings.bridgeToken()?.takeIf { it.isNotBlank() }?.let {
            header(HttpHeaders.Authorization, "Bearer $it")
        }
        settings.activeProfile()?.let { header("X-Hermes-Profile", it) }
    }

    /** Verify the bridge URL + token. Throws on non-2xx. */
    suspend fun authCheck() {
        client.get("${base()}/v1/auth/check") { auth() }.body<Map<String, Boolean>>()
    }

    suspend fun tasks(): List<TaskDto> =
        client.get("${base()}/v1/tasks") { auth() }.body()

    suspend fun schedules(): List<ScheduleDto> =
        client.get("${base()}/v1/schedules") { auth() }.body()

    suspend fun mcpServers(): List<McpServerDto> =
        client.get("${base()}/v1/mcp") { auth() }.body()

    suspend fun profiles(): ProfilesResponse =
        client.get("${base()}/v1/profiles") { auth() }.body()

    suspend fun messages(sessionId: String): List<co.hermesdispatch.app.data.remote.dto.MessageDto> =
        client.get("${base()}/v1/tasks/$sessionId/messages") { auth() }.body()

    suspend fun models(): co.hermesdispatch.app.data.remote.dto.ModelsResponse =
        client.get("${base()}/v1/models") { auth() }.body()

    suspend fun pushInfo(): co.hermesdispatch.app.data.remote.dto.PushInfo =
        client.get("${base()}/v1/push/info") { auth() }.body()

    suspend fun setModel(provider: String, model: String) {
        client.post("${base()}/v1/models/set") {
            contentType(ContentType.Application.Json)
            auth()
            setBody(co.hermesdispatch.app.data.remote.dto.SetModelRequest(model, provider))
        }
    }

    suspend fun startTask(req: StartTaskRequest): StartTaskResponse =
        client.post("${base()}/v1/tasks") {
            contentType(ContentType.Application.Json)
            auth()
            setBody(req)
        }.body()

    /** Server-Sent Events for a held run. */
    fun streamTask(streamId: String) =
        sse.stream("${base()}/v1/tasks/$streamId/events") { auth() }

    suspend fun cancelTask(streamId: String) {
        client.post("${base()}/v1/tasks/$streamId/cancel") { auth() }
    }

    suspend fun steerTask(streamId: String, message: String) {
        client.post("${base()}/v1/tasks/$streamId/steer") {
            contentType(ContentType.Application.Json)
            auth()
            setBody(SteerRequest(message))
        }
    }

    suspend fun approveTask(streamId: String, choice: String) {
        client.post("${base()}/v1/tasks/$streamId/approve") {
            contentType(ContentType.Application.Json)
            auth()
            setBody(co.hermesdispatch.app.data.remote.dto.ApproveRequest(choice))
        }
    }

    suspend fun clarifyTask(streamId: String, answer: String) {
        client.post("${base()}/v1/tasks/$streamId/clarify") {
            contentType(ContentType.Application.Json)
            auth()
            setBody(co.hermesdispatch.app.data.remote.dto.ClarifyRequest(answer))
        }
    }

    suspend fun scheduleAction(action: String, id: String) {
        client.post("${base()}/v1/schedules/$id/$action") { auth() }
    }

    suspend fun updateSchedule(
        id: String,
        req: co.hermesdispatch.app.data.remote.dto.ScheduleUpdateRequest,
    ) {
        client.patch("${base()}/v1/schedules/$id") {
            contentType(ContentType.Application.Json)
            auth()
            setBody(req)
        }
    }
}
