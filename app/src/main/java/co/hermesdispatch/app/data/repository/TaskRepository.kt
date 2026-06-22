package co.hermesdispatch.app.data.repository

import co.hermesdispatch.app.data.local.TaskDao
import co.hermesdispatch.app.data.local.TaskEntity
import co.hermesdispatch.app.data.local.TaskLabelDao
import co.hermesdispatch.app.data.local.TaskLabelEntity
import co.hermesdispatch.app.data.remote.HermesApi
import co.hermesdispatch.app.data.remote.dto.TaskDto
import co.hermesdispatch.app.domain.Task
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class TaskRepository @Inject constructor(
    private val api: HermesApi,
    private val dao: TaskDao,
    private val labelDao: TaskLabelDao,
) {
    /** Tasks, with any user-chosen rename applied over the server title. */
    fun observeTasks(): Flow<List<Task>> =
        combine(dao.observeAll(), labelDao.observeAll()) { rows, labels ->
            val overrides = labels.associate { it.sessionId to it.label }
            rows.map {
                Task(
                    id = it.id,
                    title = overrides[it.id]?.ifBlank { null } ?: it.title,
                    status = it.status,
                    model = it.model,
                    updatedAt = it.updatedAt,
                )
            }
        }

    /** The user-chosen name for a task, if any (live). */
    fun observeLabel(sessionId: String): Flow<String?> = labelDao.observeLabel(sessionId)

    suspend fun setLabel(sessionId: String, label: String) {
        val trimmed = label.trim()
        if (trimmed.isBlank()) labelDao.delete(sessionId)
        else labelDao.upsert(TaskLabelEntity(sessionId, trimmed))
    }

    /** Pull the latest tasks from the bridge into the local cache. */
    suspend fun refresh(): Result<Unit> = runCatching {
        val remote = api.tasks().map {
            TaskEntity(
                id = it.id,
                title = it.title.ifBlank { "Untitled task" },
                status = it.status,
                model = it.model,
                updatedAt = (it.updatedAt * 1000).toLong(),
            )
        }
        dao.replaceAll(remote)
    }

    suspend fun clearCache() = dao.clear()

    /** Apply the user's local renames over a freshly-fetched (uncached) list. */
    private suspend fun withLabels(dtos: List<TaskDto>): List<Task> {
        val overrides = labelDao.observeAll().first().associate { it.sessionId to it.label }
        return dtos.map {
            Task(
                id = it.id,
                title = overrides[it.id]?.ifBlank { null } ?: it.title.ifBlank { "Untitled task" },
                status = it.status,
                model = it.model,
                updatedAt = (it.updatedAt * 1000).toLong(),
            )
        }
    }

    /** Archived tasks (server-side flag), fetched on demand — not cached. */
    suspend fun archivedTasks(): Result<List<Task>> =
        runCatching { withLabels(api.tasks(archived = "only")) }

    /** Full-text search across sessions (server-side). */
    suspend fun search(query: String): Result<List<Task>> = runCatching {
        if (query.isBlank()) emptyList() else withLabels(api.searchTasks(query.trim()))
    }

    /** Archive/unarchive a task server-side, then refresh the active cache. */
    suspend fun setArchived(sessionId: String, archived: Boolean): Result<Unit> = runCatching {
        api.archiveTask(sessionId, archived)
        refresh()
    }
}
