package co.hermesdispatch.app.data.repository

import co.hermesdispatch.app.data.local.ScheduleDao
import co.hermesdispatch.app.data.local.ScheduleEntity
import co.hermesdispatch.app.data.remote.HermesApi
import co.hermesdispatch.app.domain.Schedule
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ScheduleRepository @Inject constructor(
    private val api: HermesApi,
    private val dao: ScheduleDao,
) {
    fun observeSchedules(): Flow<List<Schedule>> = dao.observeAll().map { rows ->
        rows.map {
            Schedule(
                id = it.id,
                name = it.name,
                cronExpr = it.cronExpr,
                paused = it.paused,
                nextRun = it.nextRun,
                lastRun = it.lastRun,
            )
        }
    }

    suspend fun refresh(): Result<Unit> = runCatching {
        val remote = api.schedules().map {
            ScheduleEntity(
                id = it.id,
                name = it.name.ifBlank { "Scheduled task" },
                cronExpr = it.cron,
                paused = it.paused,
                nextRun = it.nextRun?.let { ts -> (ts * 1000).toLong() },
                lastRun = it.lastRun?.let { ts -> (ts * 1000).toLong() },
            )
        }
        dao.upsertAll(remote)
    }

    suspend fun setPaused(id: String, paused: Boolean): Result<Unit> = runCatching {
        api.scheduleAction(if (paused) "pause" else "resume", id)
        refresh()
    }

    suspend fun runNow(id: String): Result<Unit> = runCatching { api.scheduleAction("run", id) }
    suspend fun delete(id: String): Result<Unit> = runCatching {
        api.scheduleAction("delete", id)
        refresh()
    }
}
