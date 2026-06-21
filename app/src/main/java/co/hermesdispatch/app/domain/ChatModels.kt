package co.hermesdispatch.app.domain

/** A turn in the conversation thread. */
data class ChatMessage(
    val id: Long,
    val role: Role,
    val text: String,
) {
    enum class Role { USER, ASSISTANT }
}

/** An entry in the live "actions" timeline (tool calls + status lines). */
data class ActionItem(
    val id: Long,
    val kind: Kind,
    val label: String,
) {
    enum class Kind { TOOL, STATUS, REASONING }
}

/** A link or image the agent surfaced, pinned above the thread. */
data class Artifact(
    val url: String,
    val isImage: Boolean,
)
