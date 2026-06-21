package co.hermesdispatch.app.ui.settings

import androidx.lifecycle.ViewModel
import co.hermesdispatch.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val bridgeUrl: String = "",
    val profile: String = "",
    val pushConfigured: Boolean = false,
    val signedOut: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(snapshot())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private fun snapshot() = SettingsUiState(
        bridgeUrl = auth.bridgeUrl().orEmpty(),
        profile = auth.activeProfile().orEmpty(),
        pushConfigured = !auth.pushEndpoint().isNullOrBlank(),
    )

    fun saveProfile(profile: String) {
        auth.setProfile(profile.trim().ifBlank { null })
        _state.value = _state.value.copy(profile = profile.trim())
    }

    fun signOut() {
        auth.signOut()
        _state.value = _state.value.copy(signedOut = true)
    }
}
