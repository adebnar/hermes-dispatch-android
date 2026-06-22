package co.hermesdispatch.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.hermesdispatch.app.data.prefs.SecureSettings
import co.hermesdispatch.app.data.repository.TaskRepository
import co.hermesdispatch.app.domain.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val settings: SecureSettings,
) : ViewModel() {

    /** Active profile this list is scoped to (shown in the top bar). */
    val activeProfile: String get() = settings.activeProfile().orEmpty()

    /** Active (non-archived) tasks, cached + live with local renames applied. */
    val tasks: StateFlow<List<Task>> = repository.observeTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<Task>>(emptyList())
    val results: StateFlow<List<Task>> = _results.asStateFlow()

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()

    private val _archived = MutableStateFlow<List<Task>>(emptyList())
    val archived: StateFlow<List<Task>> = _archived.asStateFlow()

    private var searchJob: Job? = null
    private var lastArchivedId: String? = null

    init { refresh() }

    fun refresh() {
        if (_refreshing.value) return
        _refreshing.value = true
        viewModelScope.launch {
            repository.refresh().onFailure { _error.value = it.message }
            if (_showArchived.value) loadArchived()
            _refreshing.value = false
        }
    }

    fun setQuery(q: String) {
        _query.value = q
        searchJob?.cancel()
        if (q.isBlank()) {
            _results.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(250) // debounce
            repository.search(q).onSuccess { _results.value = it }
        }
    }

    fun setShowArchived(on: Boolean) {
        _showArchived.value = on
        if (on) loadArchived()
    }

    private fun loadArchived() {
        viewModelScope.launch { repository.archivedTasks().onSuccess { _archived.value = it } }
    }

    fun archive(task: Task) {
        lastArchivedId = task.id
        viewModelScope.launch {
            repository.setArchived(task.id, true)
            if (_showArchived.value) loadArchived()
        }
    }

    fun unarchive(task: Task) {
        viewModelScope.launch {
            repository.setArchived(task.id, false)
            loadArchived()
        }
    }

    fun undo() {
        val id = lastArchivedId ?: return
        lastArchivedId = null
        viewModelScope.launch { repository.setArchived(id, false) }
    }
}
