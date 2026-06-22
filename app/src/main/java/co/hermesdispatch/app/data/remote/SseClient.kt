package co.hermesdispatch.app.data.remote

import co.hermesdispatch.app.data.remote.sse.SseParser
import co.hermesdispatch.app.data.remote.sse.StreamEvent
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readUTF8Line
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Streams a Server-Sent Events endpoint as a cold [Flow] of [StreamEvent].
 *
 * Implements the minimal SSE framing we need: `event:` sets the current event
 * name, `data:` lines accumulate, and a blank line dispatches a frame. Comment
 * lines (`:`) are ignored. Frame→event mapping is delegated to the pure
 * [SseParser] so it stays unit-testable.
 */
@Singleton
class SseClient @Inject constructor(
    private val client: HttpClient,
) {
    fun stream(url: String, configure: HttpRequestBuilder.() -> Unit = {}): Flow<StreamEvent> = flow {
        client.prepareGet(url) {
            header(HttpHeaders.Accept, "text/event-stream")
            header(HttpHeaders.CacheControl, "no-cache")
            // SSE runs as long as the agent works — disable the request/socket
            // timeouts for the stream (otherwise long tasks hit the 60s cap).
            timeout {
                requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
                socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
            }
            configure()
        }.execute { response ->
            val channel = response.bodyAsChannel()
            var event: String? = null
            val data = StringBuilder()
            while (true) {
                val line = channel.readUTF8Line() ?: break
                when {
                    line.isEmpty() -> {
                        if (data.isNotEmpty() || event != null) {
                            emit(SseParser.parse(event, data.toString().trimEnd('\n')))
                        }
                        event = null
                        data.setLength(0)
                    }
                    line.startsWith(":") -> Unit // comment / keep-alive
                    line.startsWith("event:") -> event = line.removePrefix("event:").trim()
                    line.startsWith("data:") -> data.append(line.removePrefix("data:").trim()).append('\n')
                }
            }
        }
    }
}
