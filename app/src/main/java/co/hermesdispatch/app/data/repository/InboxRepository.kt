package co.hermesdispatch.app.data.repository

import co.hermesdispatch.app.data.remote.HermesApi
import co.hermesdispatch.app.domain.InboxItem
import javax.inject.Inject
import javax.inject.Singleton

/** Reads local cron deliverables (the Inbox) and manages per-job alert subscriptions. */
@Singleton
class InboxRepository @Inject constructor(
    private val api: HermesApi,
) {
    /** Inbox items for the active profile, each tagged with its alert state. */
    suspend fun items(): Result<List<InboxItem>> = runCatching {
        val alerts = runCatching { api.inboxAlerts().jobIds.toSet() }.getOrDefault(emptySet())
        api.inbox().map {
            InboxItem(
                id = it.id,
                jobId = it.jobId,
                jobName = it.jobName,
                status = it.status,
                createdAt = it.createdAt?.let { ts -> (ts * 1000).toLong() },
                snippet = it.snippet,
                alerting = it.jobId in alerts,
            )
        }
    }

    /** Full markdown content of one item. */
    suspend fun content(id: String): Result<String> =
        runCatching { api.inboxItem(id).content }

    /** Toggle alerts for a job; returns the new alerting state for that job. */
    suspend fun setAlert(jobId: String, enabled: Boolean): Result<Boolean> = runCatching {
        val current = api.inboxAlerts().jobIds.toMutableSet()
        if (enabled) current.add(jobId) else current.remove(jobId)
        api.setInboxAlerts(current.toList()).jobIds.contains(jobId)
    }
}
