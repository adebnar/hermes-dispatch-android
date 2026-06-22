package co.hermesdispatch.app.ui.scheduled

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.hermesdispatch.app.data.repository.InboxRepository
import co.hermesdispatch.app.data.repository.ScheduleRepository
import co.hermesdispatch.app.domain.Schedule
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ScheduledViewModel @Inject constructor(
    private val repository: ScheduleRepository,
    private val inbox: InboxRepository,
) : ViewModel() {

    val schedules: StateFlow<List<Schedule>> = repository.observeSchedules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    /** Cron job ids subscribed to inbox alerts — drives the per-job bell. */
    private val _alerting = MutableStateFlow<Set<String>>(emptySet())
    val alerting: StateFlow<Set<String>> = _alerting.asStateFlow()

    fun refresh() = viewModelScope.launch {
        _refreshing.value = true
        repository.refresh()
        _alerting.value = inbox.alertJobIds()
        _refreshing.value = false
    }

    fun toggleAlert(schedule: Schedule) {
        val target = schedule.id !in _alerting.value
        // Optimistic.
        _alerting.update { if (target) it + schedule.id else it - schedule.id }
        viewModelScope.launch {
            inbox.setAlert(schedule.id, target).onSuccess { confirmed ->
                _alerting.update { if (confirmed) it + schedule.id else it - schedule.id }
            }
        }
    }

    fun togglePause(schedule: Schedule) = viewModelScope.launch {
        repository.setPaused(schedule.id, !schedule.paused)
    }

    fun runNow(schedule: Schedule) = viewModelScope.launch { repository.runNow(schedule.id) }

    fun delete(schedule: Schedule) = viewModelScope.launch { repository.delete(schedule.id) }

    fun update(schedule: Schedule, name: String, prompt: String, cron: String) =
        viewModelScope.launch {
            repository.update(schedule.id, name = name, prompt = prompt, schedule = cron)
        }
}
