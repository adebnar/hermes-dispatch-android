package co.hermesdispatch.app.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.hermesdispatch.app.data.repository.AuthRepository
import co.hermesdispatch.app.push.PushProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PairingUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
)

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val push: PushProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(PairingUiState())
    val state: StateFlow<PairingUiState> = _state.asStateFlow()

    fun connect(bridgeUrl: String, token: String, profile: String) {
        if (_state.value.loading) return
        _state.value = PairingUiState(loading = true)
        viewModelScope.launch {
            auth.pairAndConnect(
                bridgeUrl = bridgeUrl.trim(),
                token = token,
                profile = profile.trim().ifBlank { null },
            ).fold(
                onSuccess = {
                    // Best-effort: subscribe to push if a distributor (e.g. ntfy) is available.
                    runCatching { push.register() }
                    _state.value = PairingUiState(done = true)
                },
                onFailure = { _state.value = PairingUiState(error = it.message ?: "Connection failed") },
            )
        }
    }
}
