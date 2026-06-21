package co.hermesdispatch.app.data.repository

import co.hermesdispatch.app.data.remote.HermesApi
import co.hermesdispatch.app.data.remote.dto.StartTaskRequest
import co.hermesdispatch.app.data.remote.sse.StreamEvent
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

    suspend fun startRun(sessionId: String?, message: String): StartResult {
        val resp = api.startTask(StartTaskRequest(sessionId = sessionId, message = message))
        return if (resp.kind == "cron") {
            StartResult.Cron(resp.cron)
        } else {
            StartResult.Run(resp.sessionId, requireNotNull(resp.streamId) { "missing stream_id" })
        }
    }

    fun stream(streamId: String): Flow<StreamEvent> = api.streamTask(streamId)

    suspend fun cancel(streamId: String) = api.cancelTask(streamId)

    suspend fun steer(streamId: String, message: String) = api.steerTask(streamId, message)
}
