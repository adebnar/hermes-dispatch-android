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
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
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

    suspend fun tasks(archived: String = "exclude"): List<TaskDto> =
        client.get("${base()}/v1/tasks") {
            auth()
            parameter("archived", archived)
        }.body()

    suspend fun searchTasks(query: String): List<TaskDto> =
        client.get("${base()}/v1/tasks/search") {
            auth()
            parameter("q", query)
        }.body()

    suspend fun archiveTask(sessionId: String, archived: Boolean) {
        client.post("${base()}/v1/tasks/$sessionId/archive") {
            contentType(ContentType.Application.Json)
            auth()
            setBody(co.hermesdispatch.app.data.remote.dto.ArchiveRequest(archived))
        }
    }

    suspend fun schedules(): List<ScheduleDto> =
        client.get("${base()}/v1/schedules") { auth() }.body()

    suspend fun mcpServers(): List<McpServerDto> =
        client.get("${base()}/v1/mcp") { auth() }.body()

    suspend fun profiles(): ProfilesResponse =
        client.get("${base()}/v1/profiles") { auth() }.body()

    /** Bridge + gateway versions for the About screen. */
    suspend fun info(): co.hermesdispatch.app.data.remote.dto.InfoDto =
        client.get("${base()}/v1/info") { auth() }.body()

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

    /** Upload a document to the dashboard's managed-files area; returns its path. */
    suspend fun uploadFile(filename: String, dataUrl: String): String =
        client.post("${base()}/v1/files/upload") {
            contentType(ContentType.Application.Json)
            auth()
            setBody(co.hermesdispatch.app.data.remote.dto.FileUploadRequest(filename, dataUrl))
        }.body<co.hermesdispatch.app.data.remote.dto.FileUploadResponse>().path

    /** Server-Sent Events for a held run. */
    fun streamTask(streamId: String) =
        sse.stream("${base()}/v1/tasks/$streamId/events") { auth() }

    /** Whether the session has a run actively streaming server-side right now. */
    suspend fun taskLive(sessionId: String): Boolean =
        client.get("${base()}/v1/tasks/$sessionId/live") { auth() }
            .body<Map<String, Boolean>>()["live"] == true

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

    suspend fun inbox(): List<co.hermesdispatch.app.data.remote.dto.InboxItemDto> =
        client.get("${base()}/v1/inbox") { auth() }.body()

    suspend fun inboxItem(id: String): co.hermesdispatch.app.data.remote.dto.InboxContentDto =
        client.get("${base()}/v1/inbox/item") {
            auth()
            parameter("id", id)
        }.body()

    suspend fun inboxAlerts(): co.hermesdispatch.app.data.remote.dto.AlertsResponse =
        client.get("${base()}/v1/inbox/alerts") { auth() }.body()

    suspend fun setInboxAlerts(
        jobIds: List<String>,
        alertOnFailures: Boolean? = null,
    ): co.hermesdispatch.app.data.remote.dto.AlertsResponse =
        client.put("${base()}/v1/inbox/alerts") {
            contentType(ContentType.Application.Json)
            auth()
            setBody(co.hermesdispatch.app.data.remote.dto.AlertsRequest(jobIds, alertOnFailures))
        }.body()

    suspend fun setPushKey(key: String) {
        client.post("${base()}/v1/push/key") {
            contentType(ContentType.Application.Json)
            auth()
            setBody(co.hermesdispatch.app.data.remote.dto.PushKeyRequest(key))
        }
    }

    suspend fun transcribe(
        audio: ByteArray,
        filename: String = "audio.m4a",
    ): co.hermesdispatch.app.data.remote.dto.TranscribeResponse =
        client.post("${base()}/v1/audio/transcribe") {
            auth()
            setBody(
                io.ktor.client.request.forms.MultiPartFormDataContent(
                    io.ktor.client.request.forms.formData {
                        append(
                            "file",
                            audio,
                            io.ktor.http.Headers.build {
                                append(HttpHeaders.ContentType, "audio/mp4")
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "filename=\"$filename\"",
                                )
                            },
                        )
                    },
                ),
            )
        }.body()
}
