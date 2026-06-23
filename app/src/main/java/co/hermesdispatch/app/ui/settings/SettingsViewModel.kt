package co.hermesdispatch.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.hermesdispatch.app.data.repository.AuthRepository
import co.hermesdispatch.app.data.repository.InboxRepository
import co.hermesdispatch.app.data.repository.ScheduleRepository
import co.hermesdispatch.app.data.repository.TaskRepository
import co.hermesdispatch.app.diag.DiagnosticReporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val serverStt: Boolean = false,
    val encryptedPush: Boolean = false,
    val alertOnFailures: Boolean = false,
    val bugReporting: Boolean = false,
    val reportPreview: String? = null,
    val preparingReport: Boolean = false,
    val alertSoundTitle: String = "Default",
    val appVersion: String = "",
    val gatewayVersion: String = "",
    val bridgeVersion: String = "",
    val signedOut: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val tasks: TaskRepository,
    private val schedules: ScheduleRepository,
    private val inbox: InboxRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(snapshot())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(availableProfiles = auth.availableProfiles()) }
        }
        viewModelScope.launch {
            _state.update { it.copy(alertOnFailures = inbox.alertOnFailures()) }
        }
        viewModelScope.launch {
            auth.pushInfo()?.let { p ->
                _state.update {
                    it.copy(pushConfigured = p.configured, pushTopic = p.topic, pushBaseUrl = p.baseUrl)
                }
            }
        }
        viewModelScope.launch {
            auth.serverInfo()?.let { info ->
                _state.update {
                    it.copy(
                        gatewayVersion = info.gatewayVersion.orEmpty(),
                        bridgeVersion = info.bridgeVersion.orEmpty(),
                    )
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
        serverStt = auth.serverTranscription(),
        encryptedPush = auth.encryptedPushEnabled(),
        bugReporting = auth.bugReporting(),
        alertSoundTitle = soundTitle(),
        appVersion = co.hermesdispatch.app.BuildConfig.VERSION_NAME,
    )

    private fun soundTitle(): String = when (val pref = auth.alertSoundUri()) {
        null -> "Default"
        "" -> "Silent"
        else -> runCatching {
            android.media.RingtoneManager.getRingtone(appContext, android.net.Uri.parse(pref))
                ?.getTitle(appContext)
        }.getOrNull() ?: "Custom"
    }

    /** Apply a sound chosen from the system ringtone picker (null uri = Silent). */
    fun onAlertSoundPicked(uri: android.net.Uri?) {
        co.hermesdispatch.app.push.NotificationHelper.applyAlertSound(
            appContext, uri, silent = uri == null,
        )
        _state.update { it.copy(alertSoundTitle = soundTitle()) }
    }

    fun setBugReporting(on: Boolean) {
        auth.setBugReporting(on)
        _state.update { it.copy(bugReporting = on) }
    }

    /** Collect + redact a diagnostic report off the main thread, then show a preview. */
    fun prepareReport() {
        _state.update { it.copy(preparingReport = true) }
        viewModelScope.launch {
            val report = withContext(Dispatchers.IO) {
                DiagnosticReporter.build(appContext, auth.secretsToMask())
            }
            _state.update { it.copy(preparingReport = false, reportPreview = report) }
        }
    }

    fun dismissReport() = _state.update { it.copy(reportPreview = null) }

    fun setServerStt(on: Boolean) {
        auth.setServerTranscription(on)
        _state.update { it.copy(serverStt = on) }
    }

    fun setEncryptedPush(on: Boolean) {
        _state.update { it.copy(encryptedPush = on) } // optimistic
        viewModelScope.launch {
            auth.setEncryptedPush(on).onFailure {
                _state.update { s -> s.copy(encryptedPush = auth.encryptedPushEnabled()) }
            }
        }
    }

    fun setAlertOnFailures(on: Boolean) {
        _state.update { it.copy(alertOnFailures = on) } // optimistic
        viewModelScope.launch {
            inbox.setAlertOnFailures(on)
                .onSuccess { confirmed -> _state.update { it.copy(alertOnFailures = confirmed) } }
                .onFailure { _state.update { it.copy(alertOnFailures = inbox.alertOnFailures()) } }
        }
    }

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
