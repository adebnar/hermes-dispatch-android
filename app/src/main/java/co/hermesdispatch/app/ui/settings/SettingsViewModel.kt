package co.hermesdispatch.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.hermesdispatch.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val bridgeUrl: String = "",
    val profile: String = "",
    val availableProfiles: List<String> = emptyList(),
    val pushConfigured: Boolean = false,
    val signedOut: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(snapshot())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val profiles = auth.availableProfiles()
            _state.update { it.copy(availableProfiles = profiles) }
        }
    }

    private fun snapshot() = SettingsUiState(
        bridgeUrl = auth.bridgeUrl().orEmpty(),
        profile = auth.activeProfile().orEmpty(),
        pushConfigured = !auth.pushEndpoint().isNullOrBlank(),
    )

    fun saveProfile(profile: String) {
        auth.setProfile(profile.trim().ifBlank { null })
        _state.update { it.copy(profile = profile.trim()) }
    }

    fun signOut() {
        auth.signOut()
        _state.value = _state.value.copy(signedOut = true)
    }
}
