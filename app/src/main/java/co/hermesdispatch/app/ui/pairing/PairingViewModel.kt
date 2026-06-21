package co.hermesdispatch.app.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.hermesdispatch.app.data.repository.AuthRepository
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
) : ViewModel() {

    private val _state = MutableStateFlow(PairingUiState())
    val state: StateFlow<PairingUiState> = _state.asStateFlow()

    fun connect(bridgeUrl: String, password: String, profile: String) {
        if (_state.value.loading) return
        _state.value = PairingUiState(loading = true)
        viewModelScope.launch {
            auth.pairAndLogin(
                bridgeUrl = bridgeUrl.trim(),
                password = password,
                profile = profile.trim().ifBlank { null },
            ).fold(
                onSuccess = { _state.value = PairingUiState(done = true) },
                onFailure = { _state.value = PairingUiState(error = it.message ?: "Connection failed") },
            )
        }
    }
}
