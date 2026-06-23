package co.hermesdispatch.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Shapes of the Hermes Dispatch bridge `/v1` API. */

@Serializable
data class TaskDto(
    val id: String,
    val title: String = "",
    val status: String = "",
    val model: String? = null,
    @SerialName("updated_at") val updatedAt: Double = 0.0,
)

@Serializable
data class ScheduleDto(
    val id: String,
    val name: String = "",
    val cron: String = "",
    val prompt: String = "",
    val paused: Boolean = false,
    @SerialName("next_run") val nextRun: Double? = null,
    @SerialName("last_run") val lastRun: Double? = null,
)

@Serializable
data class ScheduleUpdateRequest(
    val name: String? = null,
    val prompt: String? = null,
    val schedule: String? = null,
)

@Serializable
data class McpServerDto(
    val name: String,
    val status: String? = null,
    @SerialName("tools_count") val toolsCount: Int? = null,
)

@Serializable
data class StartTaskRequest(
    val message: String,
    @SerialName("session_id") val sessionId: String? = null,
    val model: String? = null,
    val images: List<String> = emptyList(),
)

@Serializable
data class StartTaskResponse(
    val kind: String, // "oneshot" | "cron"
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("stream_id") val streamId: String? = null,
    val cron: String? = null,
)

@Serializable
data class SteerRequest(val message: String)

@Serializable
data class ApproveRequest(val choice: String, val all: Boolean = false)

@Serializable
data class ClarifyRequest(val answer: String)

@Serializable
data class MessageDto(val role: String, val text: String)

@Serializable
data class InfoDto(
    @SerialName("bridge_version") val bridgeVersion: String? = null,
    @SerialName("gateway_version") val gatewayVersion: String? = null,
)

@Serializable
data class ModelOptionDto(val provider: String, val model: String)

@Serializable
data class ModelsResponse(
    val models: List<ModelOptionDto> = emptyList(),
    val current: String? = null,
)

@Serializable
data class SetModelRequest(val model: String, val provider: String? = null)

@Serializable
data class PushInfo(
    val configured: Boolean = false,
    @SerialName("base_url") val baseUrl: String = "",
    val topic: String = "",
)

@Serializable
data class ProfileDto(
    val name: String,
    val model: String? = null,
    @SerialName("is_default") val isDefault: Boolean = false,
)

@Serializable
data class ProfilesResponse(
    val profiles: List<ProfileDto> = emptyList(),
    val active: String? = null,
)

@Serializable
data class InboxItemDto(
    val id: String,
    val profile: String = "",
    @SerialName("job_id") val jobId: String = "",
    @SerialName("job_name") val jobName: String = "",
    val title: String = "",
    val status: String = "ok",
    @SerialName("created_at") val createdAt: Double? = null,
    val size: Int = 0,
    val snippet: String = "",
)

@Serializable
data class InboxContentDto(
    val id: String,
    val profile: String = "",
    @SerialName("job_id") val jobId: String = "",
    @SerialName("job_name") val jobName: String = "",
    val title: String = "",
    val status: String = "ok",
    @SerialName("created_at") val createdAt: Double? = null,
    val size: Int = 0,
    val snippet: String = "",
    val output: String = "",
    val content: String = "",
)

@Serializable
data class AlertsRequest(
    @SerialName("job_ids") val jobIds: List<String> = emptyList(),
    @SerialName("alert_on_failures") val alertOnFailures: Boolean? = null,
)

@Serializable
data class AlertsResponse(
    @SerialName("job_ids") val jobIds: List<String> = emptyList(),
    @SerialName("alert_on_failures") val alertOnFailures: Boolean = false,
)

@Serializable
data class PushKeyRequest(val key: String = "")

@Serializable
data class ArchiveRequest(val archived: Boolean = true)

@Serializable
data class TranscribeResponse(val text: String = "")
