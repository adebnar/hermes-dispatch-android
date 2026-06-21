package co.hermesdispatch.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val password: String)

@Serializable
data class LoginResponse(val ok: Boolean = false)

@Serializable
data class SessionDto(
    @SerialName("session_id") val sessionId: String,
    val title: String = "",
    val model: String? = null,
    @SerialName("model_provider") val modelProvider: String? = null,
    val status: String? = null,
    @SerialName("updated_at") val updatedAt: Double = 0.0,
)

@Serializable
data class CronDto(
    val id: String,
    val name: String = "",
    val schedule: String = "",
    val enabled: Boolean = true,
    @SerialName("next_run_at") val nextRunAt: Double? = null,
    @SerialName("last_run_at") val lastRunAt: Double? = null,
    val workspace: String? = null,
)

@Serializable
data class McpServerDto(
    val name: String,
    val status: String? = null,
    @SerialName("tools_count") val toolsCount: Int? = null,
)

@Serializable
data class ChatStartRequest(
    @SerialName("session_id") val sessionId: String? = null,
    val message: String,
    val model: String? = null,
    val workspace: String? = null,
)

@Serializable
data class ChatStartResponse(
    @SerialName("stream_id") val streamId: String,
)
