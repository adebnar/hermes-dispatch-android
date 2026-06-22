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
    val paused: Boolean = false,
    @SerialName("next_run") val nextRun: Double? = null,
    @SerialName("last_run") val lastRun: Double? = null,
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
data class MessageDto(val role: String, val text: String)

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
