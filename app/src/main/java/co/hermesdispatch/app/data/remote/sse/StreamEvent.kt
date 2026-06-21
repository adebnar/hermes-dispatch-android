package co.hermesdispatch.app.data.remote.sse

/**
 * Typed view of the Hermes (webui) SSE stream.
 *
 * The upstream contract is not formally stable, so the parser is intentionally
 * tolerant: every known event maps to a case below and anything unrecognized
 * becomes [Unknown] rather than throwing. Event vocabulary observed in
 * hermes-webui `api/streaming.py`: status, message/text/token/delta, tool/
 * tool_use, reasoning/thinking, assistant_started, worker_started, completed/
 * done, interrupted, cancelled, error, usage, clarify, approval, todo_state,
 * image. See docs/API-CONTRACT.md.
 */
sealed interface StreamEvent {
    /** Incremental assistant text. */
    data class Token(val text: String) : StreamEvent

    /** A tool/MCP invocation started (drives the "actions" pane). */
    data class Tool(val name: String, val preview: String?) : StreamEvent

    /** Free-form status line ("Searching Web", "Using MCP: gmail"). */
    data class Status(val text: String) : StreamEvent

    /** Model reasoning / extended-thinking delta. */
    data class Reasoning(val text: String) : StreamEvent

    /** A permission/approval prompt requiring user input. */
    data class Approval(val command: String, val description: String?) : StreamEvent

    /** The agent asked the user a clarifying question mid-run. */
    data class Clarify(val question: String) : StreamEvent

    /** Terminal success. */
    data class Completed(val raw: String?) : StreamEvent

    /** Terminal failure. */
    data class Error(val message: String, val type: String?) : StreamEvent

    /** Run was interrupted or cancelled. */
    data object Interrupted : StreamEvent

    /** Recognized framing but no actionable payload, or an unknown event name. */
    data class Unknown(val event: String, val data: String) : StreamEvent
}
