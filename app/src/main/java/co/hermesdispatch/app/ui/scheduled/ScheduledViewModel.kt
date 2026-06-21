package co.hermesdispatch.app.ui.scheduled

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.hermesdispatch.app.data.repository.ScheduleRepository
import co.hermesdispatch.app.domain.Schedule
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ScheduledViewModel @Inject constructor(
    private val repository: ScheduleRepository,
) : ViewModel() {

    val schedules: StateFlow<List<Schedule>> = repository.observeSchedules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { refresh() }

    fun refresh() = viewModelScope.launch { repository.refresh() }

    fun togglePause(schedule: Schedule) = viewModelScope.launch {
        repository.setPaused(schedule.id, !schedule.paused)
    }

    fun runNow(schedule: Schedule) = viewModelScope.launch { repository.runNow(schedule.id) }

    fun delete(schedule: Schedule) = viewModelScope.launch { repository.delete(schedule.id) }
}
