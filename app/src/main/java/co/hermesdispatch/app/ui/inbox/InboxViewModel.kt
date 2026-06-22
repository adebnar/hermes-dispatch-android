package co.hermesdispatch.app.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.hermesdispatch.app.data.repository.InboxRepository
import co.hermesdispatch.app.domain.InboxItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InboxViewModel @Inject constructor(
    private val repository: InboxRepository,
) : ViewModel() {

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()

    val items: StateFlow<List<InboxItem>> = _showArchived
        .flatMapLatest { repository.observeItems(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Last archived/deleted id, for one-tap Undo. */
    private var lastDismissed: String? = null

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            repository.refresh().onFailure { _error.value = it.message ?: "Couldn't load inbox" }
                .onSuccess { _error.value = null }
            _refreshing.value = false
        }
    }

    fun setShowArchived(on: Boolean) { _showArchived.value = on }

    fun toggleAlert(item: InboxItem) {
        viewModelScope.launch { repository.setAlert(item.jobId, !item.alerting) }
    }

    fun togglePin(item: InboxItem) {
        viewModelScope.launch { repository.setPinned(item.id, !item.pinned) }
    }

    fun archive(item: InboxItem) {
        lastDismissed = item.id
        viewModelScope.launch { repository.archive(item.id) }
    }

    fun delete(item: InboxItem) {
        lastDismissed = item.id
        viewModelScope.launch { repository.delete(item.id) }
    }

    fun undo() {
        val id = lastDismissed ?: return
        lastDismissed = null
        viewModelScope.launch { repository.restore(id) }
    }

    fun restore(item: InboxItem) {
        viewModelScope.launch { repository.restore(item.id) }
    }

    fun markRead(id: String) {
        viewModelScope.launch { repository.markRead(id) }
    }
}
