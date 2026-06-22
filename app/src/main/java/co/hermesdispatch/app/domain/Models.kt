package co.hermesdispatch.app.domain

/** Core domain models, decoupled from transport/storage representations. */

enum class TaskKind { ONESHOT, SCHEDULED }

data class Task(
    val id: String,
    val title: String,
    val kind: TaskKind = TaskKind.ONESHOT,
    val status: String = "",
    val model: String? = null,
    val primaryTool: String? = null,
    val updatedAt: Long = 0L,
)

data class Schedule(
    val id: String,
    val name: String,
    val cronExpr: String,
    val prompt: String = "",
    val paused: Boolean = false,
    val nextRun: Long? = null,
    val lastRun: Long? = null,
    val primaryTool: String? = null,
)

/** A locally-delivered cron result (saved-to-disk markdown), shown in the Inbox. */
data class InboxItem(
    val id: String,
    val jobId: String,
    val jobName: String,
    val title: String = "",
    val status: String = "ok", // ok | failed | silent
    val createdAt: Long? = null,
    val snippet: String = "",
    val alerting: Boolean = false, // per-job push bell
    val pinned: Boolean = false,
    val unread: Boolean = true,
) {
    /** What to show as the headline — the result's title, falling back to the job. */
    val display: String get() = title.ifBlank { jobName }
}

/** A connected Hermes bridge + the active profile. */
data class Account(
    val bridgeUrl: String,
    val activeProfile: String? = null,
)
