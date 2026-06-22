package co.hermesdispatch.app.data.repository

import co.hermesdispatch.app.data.local.InboxItemStateEntity
import co.hermesdispatch.app.data.local.InboxStateDao
import co.hermesdispatch.app.data.remote.HermesApi
import co.hermesdispatch.app.domain.InboxItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * Reads local cron deliverables (the Inbox), overlays app-local triage state
 * (pin / archive / delete / read), and manages alert subscriptions. The server
 * snapshot is cached in [_server] so pin/archive/read changes (which live only
 * in Room) re-render instantly without a network round-trip.
 */
@Singleton
class InboxRepository @Inject constructor(
    private val api: HermesApi,
    private val stateDao: InboxStateDao,
) {
    private val _server = MutableStateFlow<List<InboxItem>>(emptyList())

    /**
     * Items with local state applied. [showArchived] = false shows the active
     * inbox (pins first); true shows only archived items. Deleted items are
     * always hidden.
     */
    fun observeItems(showArchived: Boolean): Flow<List<InboxItem>> =
        combine(_server, stateDao.observeAll()) { server, states ->
            val byId = states.associateBy { it.id }
            server.mapNotNull { raw ->
                val st = byId[raw.id]
                if (st?.deleted == true) return@mapNotNull null
                if ((st?.archived == true) != showArchived) return@mapNotNull null
                raw.copy(pinned = st?.pinned == true, unread = st?.read != true)
            }.sortedWith(
                compareByDescending<InboxItem> { it.pinned }.thenByDescending { it.createdAt ?: 0L },
            )
        }

    /** Pull the latest deliverables + per-job alert subscriptions from the bridge. */
    suspend fun refresh(): Result<Unit> = runCatching {
        val alerts = runCatching { api.inboxAlerts().jobIds.toSet() }.getOrDefault(emptySet())
        _server.value = api.inbox().map {
            InboxItem(
                id = it.id,
                jobId = it.jobId,
                jobName = it.jobName,
                title = it.title,
                status = it.status,
                createdAt = it.createdAt?.let { ts -> (ts * 1000).toLong() },
                snippet = it.snippet,
                alerting = it.jobId in alerts,
            )
        }
    }

    /** Full result (output) + raw markdown of one item. */
    suspend fun content(id: String): Result<Pair<String, String>> = runCatching {
        val dto = api.inboxItem(id)
        dto.output.ifBlank { dto.content } to dto.content
    }

    // --- per-item local triage state ---
    private suspend fun mutate(id: String, block: (InboxItemStateEntity) -> InboxItemStateEntity) {
        val current = stateDao.get(id) ?: InboxItemStateEntity(id)
        stateDao.upsert(block(current))
    }

    suspend fun setPinned(id: String, pinned: Boolean) = mutate(id) { it.copy(pinned = pinned) }
    suspend fun archive(id: String) = mutate(id) { it.copy(archived = true) }
    suspend fun restore(id: String) = mutate(id) { it.copy(archived = false, deleted = false) }
    suspend fun delete(id: String) = mutate(id) { it.copy(deleted = true) }
    suspend fun markRead(id: String) = mutate(id) { it.copy(read = true) }

    // --- alert subscriptions ---
    /** Toggle the per-job push bell; returns the new state for that job. */
    suspend fun setAlert(jobId: String, enabled: Boolean): Result<Boolean> = runCatching {
        val resp = api.inboxAlerts()
        val jobs = resp.jobIds.toMutableSet()
        if (enabled) jobs.add(jobId) else jobs.remove(jobId)
        val updated = api.setInboxAlerts(jobs.toList(), resp.alertOnFailures)
        // Reflect into the cached snapshot so the bell flips immediately.
        val nowAlerting = jobId in updated.jobIds
        _server.value = _server.value.map {
            if (it.jobId == jobId) it.copy(alerting = nowAlerting) else it
        }
        nowAlerting
    }

    suspend fun alertOnFailures(): Boolean =
        runCatching { api.inboxAlerts().alertOnFailures }.getOrDefault(false)

    suspend fun setAlertOnFailures(enabled: Boolean): Result<Boolean> = runCatching {
        val resp = api.inboxAlerts()
        api.setInboxAlerts(resp.jobIds, enabled).alertOnFailures
    }
}
