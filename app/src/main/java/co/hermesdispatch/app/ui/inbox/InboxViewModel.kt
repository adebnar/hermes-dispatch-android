package co.hermesdispatch.app.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.hermesdispatch.app.data.repository.InboxRepository
import co.hermesdispatch.app.domain.InboxItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InboxUiState(
    val items: List<InboxItem> = emptyList(),
    val refreshing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val repository: InboxRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(InboxUiState())
    val state: StateFlow<InboxUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true, error = null) }
            repository.items()
                .onSuccess { items -> _state.update { it.copy(items = items, refreshing = false) } }
                .onFailure { e ->
                    _state.update { it.copy(refreshing = false, error = e.message ?: "Couldn't load inbox") }
                }
        }
    }

    fun toggleAlert(item: InboxItem) {
        // Optimistic flip for this job's items.
        val target = !item.alerting
        _state.update { s ->
            s.copy(items = s.items.map { if (it.jobId == item.jobId) it.copy(alerting = target) else it })
        }
        viewModelScope.launch {
            repository.setAlert(item.jobId, target).onSuccess { confirmed ->
                _state.update { s ->
                    s.copy(items = s.items.map {
                        if (it.jobId == item.jobId) it.copy(alerting = confirmed) else it
                    })
                }
            }
        }
    }
}
