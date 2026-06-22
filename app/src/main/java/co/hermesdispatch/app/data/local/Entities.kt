package co.hermesdispatch.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val status: String,
    val model: String?,
    val updatedAt: Long,
)

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val cronExpr: String,
    val prompt: String = "",
    val paused: Boolean,
    val nextRun: Long?,
    val lastRun: Long?,
)

/**
 * A user-chosen display name for a task, kept in its own table so it survives
 * the destructive [TaskDao.replaceAll] that re-syncs tasks from the bridge.
 */
@Entity(tableName = "task_labels")
data class TaskLabelEntity(
    @PrimaryKey val sessionId: String,
    val label: String,
)

/**
 * App-local triage state for an Inbox item, keyed on its stable id
 * (`{profile}/{job}/{file}`). The underlying .md on disk is never touched —
 * archive/delete only affect what the app shows.
 */
@Entity(tableName = "inbox_item_state")
data class InboxItemStateEntity(
    @PrimaryKey val id: String,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val deleted: Boolean = false,
    val read: Boolean = false,
)
