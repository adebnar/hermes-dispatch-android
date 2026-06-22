package co.hermesdispatch.app.ui.inbox

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.hermesdispatch.app.data.repository.InboxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InboxItemUiState(
    val loading: Boolean = true,
    val output: String = "",
    val raw: String = "",
    val error: String? = null,
)

@HiltViewModel
class InboxItemViewModel @Inject constructor(
    private val repository: InboxRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val title: String = savedStateHandle.get<String>("title").orEmpty()
    private val id: String = savedStateHandle.get<String>("id").orEmpty()

    private val _state = MutableStateFlow(InboxItemUiState())
    val state: StateFlow<InboxItemUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.content(id)
                .onSuccess { (output, raw) ->
                    _state.update { it.copy(loading = false, output = output, raw = raw) }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message ?: "Couldn't open item") }
                }
        }
    }
}
