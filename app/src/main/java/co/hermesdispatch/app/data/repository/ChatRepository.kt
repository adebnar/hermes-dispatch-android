package co.hermesdispatch.app.data.repository

import co.hermesdispatch.app.data.remote.HermesApi
import co.hermesdispatch.app.data.remote.dto.ChatStartRequest
import co.hermesdispatch.app.data.remote.sse.StreamEvent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class ChatRepository @Inject constructor(
    private val api: HermesApi,
) {
    data class RunHandle(val sessionId: String, val streamId: String)

    /**
     * Begin a run. If [sessionId] is null a new session is created first so the
     * task shows up in the Tasks list. Returns identifiers needed to stream and
     * to cancel/steer.
     */
    suspend fun startRun(sessionId: String?, message: String): RunHandle {
        val sid = sessionId ?: api.createSession()
        val streamId = api.startChat(ChatStartRequest(sessionId = sid, message = message))
        return RunHandle(sessionId = sid, streamId = streamId)
    }

    fun stream(streamId: String): Flow<StreamEvent> = api.streamChat(streamId)

    suspend fun cancel(streamId: String) = api.cancelChat(streamId)

    suspend fun steer(streamId: String, message: String) = api.steerChat(streamId, message)
}
