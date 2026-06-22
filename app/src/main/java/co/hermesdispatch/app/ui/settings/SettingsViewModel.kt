package co.hermesdispatch.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.hermesdispatch.app.data.repository.AuthRepository
import co.hermesdispatch.app.data.repository.ScheduleRepository
import co.hermesdispatch.app.data.repository.TaskRepository
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
    val currentModel: String = "",
    val models: List<co.hermesdispatch.app.data.remote.dto.ModelOptionDto> = emptyList(),
    val pushConfigured: Boolean = false,
    val pushTopic: String = "",
    val pushBaseUrl: String = "",
    val savingConnection: Boolean = false,
    val connectionError: String? = null,
    val connectionSaved: Boolean = false,
    val signedOut: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val tasks: TaskRepository,
    private val schedules: ScheduleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(snapshot())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(availableProfiles = auth.availableProfiles()) }
        }
        viewModelScope.launch {
            auth.pushInfo()?.let { p ->
                _state.update {
                    it.copy(pushConfigured = p.configured, pushTopic = p.topic, pushBaseUrl = p.baseUrl)
                }
            }
        }
        loadModels()
    }

    private fun loadModels() {
        viewModelScope.launch {
            auth.models()?.let { m ->
                _state.update { it.copy(models = m.models, currentModel = m.current.orEmpty()) }
            }
        }
    }

    fun setModel(option: co.hermesdispatch.app.data.remote.dto.ModelOptionDto) {
        viewModelScope.launch {
            _state.update { it.copy(currentModel = option.model) }  // optimistic
            auth.setModel(option.provider, option.model)
            loadModels()
        }
    }

    private fun snapshot() = SettingsUiState(
        bridgeUrl = auth.bridgeUrl().orEmpty(),
        profile = auth.activeProfile().orEmpty(),
        pushConfigured = !auth.pushEndpoint().isNullOrBlank(),
    )

    /** Persist + verify a new Bridge URL / token. Blank token keeps the old one. */
    fun saveConnection(url: String, token: String) {
        viewModelScope.launch {
            _state.update { it.copy(savingConnection = true, connectionError = null, connectionSaved = false) }
            val result = auth.updateConnection(url.trim(), token)
            result
                .onSuccess {
                    _state.update {
                        it.copy(
                            savingConnection = false,
                            connectionSaved = true,
                            bridgeUrl = auth.bridgeUrl().orEmpty(),
                        )
                    }
                    // Re-pull everything against the (possibly new) bridge.
                    tasks.clearCache(); schedules.clearCache()
                    tasks.refresh(); schedules.refresh()
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(savingConnection = false, connectionError = e.message ?: "Couldn't connect")
                    }
                }
        }
    }

    fun ackConnectionSaved() = _state.update { it.copy(connectionSaved = false) }

    fun saveProfile(profile: String) {
        auth.setProfile(profile.trim().ifBlank { null })
        _state.update { it.copy(profile = profile.trim()) }
        // Switching profiles must not show the previous profile's tasks: clear the
        // caches immediately, then refetch scoped to the new profile.
        viewModelScope.launch {
            tasks.clearCache()
            schedules.clearCache()
            tasks.refresh()
            schedules.refresh()
        }
        loadModels()  // model is per-profile
    }

    fun signOut() {
        viewModelScope.launch {
            tasks.clearCache()
            schedules.clearCache()
        }
        auth.signOut()
        _state.value = _state.value.copy(signedOut = true)
    }
}
