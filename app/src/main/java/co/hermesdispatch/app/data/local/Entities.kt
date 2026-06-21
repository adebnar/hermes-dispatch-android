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
    val paused: Boolean,
    val nextRun: Long?,
    val lastRun: Long?,
)
