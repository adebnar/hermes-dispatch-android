package co.hermesdispatch.app.data.repository

import co.hermesdispatch.app.data.local.TaskDao
import co.hermesdispatch.app.data.local.TaskEntity
import co.hermesdispatch.app.data.remote.HermesApi
import co.hermesdispatch.app.domain.Task
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class TaskRepository @Inject constructor(
    private val api: HermesApi,
    private val dao: TaskDao,
) {
    fun observeTasks(): Flow<List<Task>> = dao.observeAll().map { rows ->
        rows.map { Task(id = it.id, title = it.title, status = it.status, model = it.model, updatedAt = it.updatedAt) }
    }

    /** Pull the latest sessions from the bridge into the local cache. */
    suspend fun refresh(): Result<Unit> = runCatching {
        val remote = api.sessions().map {
            TaskEntity(
                id = it.sessionId,
                title = it.title.ifBlank { "Untitled task" },
                status = it.status.orEmpty(),
                model = it.model,
                updatedAt = (it.updatedAt * 1000).toLong(),
            )
        }
        dao.upsertAll(remote)
    }
}
