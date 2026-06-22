package co.hermesdispatch.app.data.repository

import co.hermesdispatch.app.data.remote.HermesApi
import co.hermesdispatch.app.data.remote.dto.StartTaskRequest
import co.hermesdispatch.app.data.remote.sse.StreamEvent
import co.hermesdispatch.app.domain.ChatMessage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class ChatRepository @Inject constructor(
    private val api: HermesApi,
) {
    /** Result of starting a task. A recurring task becomes a [Cron]; a one-shot
     *  task becomes a streamable [Run]. */
    sealed interface StartResult {
        data class Run(val sessionId: String?, val streamId: String) : StartResult
        data class Cron(val cron: String?) : StartResult
    }

    suspend fun startRun(
        sessionId: String?,
        message: String,
        images: List<String> = emptyList(),
    ): StartResult {
        val resp = api.startTask(
            StartTaskRequest(sessionId = sessionId, message = message, images = images),
        )
        return if (resp.kind == "cron") {
            StartResult.Cron(resp.cron)
        } else {
            StartResult.Run(resp.sessionId, requireNotNull(resp.streamId) { "missing stream_id" })
        }
    }

    /** Past messages for an existing task, as (role, text) pairs. */
    suspend fun history(sessionId: String): List<Pair<ChatMessage.Role, String>> =
        api.messages(sessionId).map { dto ->
            val role = if (dto.role == "user") ChatMessage.Role.USER else ChatMessage.Role.ASSISTANT
            role to dto.text
        }

    fun stream(streamId: String): Flow<StreamEvent> = api.streamTask(streamId)

    suspend fun cancel(streamId: String) = api.cancelTask(streamId)

    suspend fun steer(streamId: String, message: String) = api.steerTask(streamId, message)

    suspend fun approve(streamId: String, choice: String) = api.approveTask(streamId, choice)

    suspend fun clarify(streamId: String, answer: String) = api.clarifyTask(streamId, answer)

    /** Server-side speech-to-text: upload recorded audio, get the transcript. */
    suspend fun transcribe(audio: ByteArray): String = api.transcribe(audio).text

    /** Available models for the active profile (for the per-session model picker). */
    suspend fun models(): List<co.hermesdispatch.app.data.remote.dto.ModelOptionDto> =
        api.models().models
}
