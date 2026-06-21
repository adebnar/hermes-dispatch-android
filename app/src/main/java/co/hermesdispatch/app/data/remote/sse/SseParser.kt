package co.hermesdispatch.app.data.remote.sse

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Pure, side-effect-free mapping from a raw SSE frame to a [StreamEvent].
 *
 * Kept separate from any I/O so it is fully unit-testable on the JVM. The
 * `event:` name may be absent (some servers only set a `type` field inside the
 * JSON payload), so both are consulted.
 */
object SseParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(eventName: String?, data: String): StreamEvent {
        val obj: JsonObject? = runCatching {
            json.parseToJsonElement(data) as? JsonObject
        }.getOrNull()

        val type = (eventName?.takeIf { it.isNotBlank() }
            ?: obj?.str("type")
            ?: "").lowercase()

        return when (type) {
            "token", "text", "delta", "message" -> {
                val text = obj?.str("text") ?: obj?.str("content") ?: data
                StreamEvent.Token(text)
            }
            "tool", "tool_use", "tool_call" -> StreamEvent.Tool(
                name = obj?.str("name") ?: obj?.str("tool_name") ?: "tool",
                preview = obj?.str("preview") ?: obj?.str("args"),
            )
            "status", "worker_started", "assistant_started" ->
                StreamEvent.Status(obj?.str("text") ?: obj?.str("label") ?: type)
            "reasoning", "thinking" ->
                StreamEvent.Reasoning(obj?.str("text") ?: data)
            "approval" -> StreamEvent.Approval(
                command = obj?.str("command") ?: "",
                description = obj?.str("description"),
            )
            "clarify" -> StreamEvent.Clarify(obj?.str("question") ?: obj?.str("prompt") ?: "")
            "completed", "done", "finished" -> StreamEvent.Completed(data)
            "interrupted", "cancelled" -> StreamEvent.Interrupted
            "error" -> StreamEvent.Error(
                message = obj?.str("message") ?: obj?.str("error") ?: "Unknown error",
                type = obj?.str("type"),
            )
            else -> StreamEvent.Unknown(eventName ?: type, data)
        }
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.let { if (it.isString) it.contentOrNull else it.content }
            ?: (this[key]?.jsonPrimitive?.contentOrNull)
}
