package co.hermesdispatch.app.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.hermesdispatch.app.data.remote.sse.StreamEvent
import co.hermesdispatch.app.data.repository.ChatRepository
import co.hermesdispatch.app.domain.ActionItem
import co.hermesdispatch.app.domain.Artifact
import co.hermesdispatch.app.domain.Artifacts
import co.hermesdispatch.app.domain.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val actions: List<ActionItem> = emptyList(),
    val artifacts: List<Artifact> = emptyList(),
    val toolsUsed: Set<String> = emptySet(),
    val running: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // "new" => no session yet; the server creates one on first send.
    private var sessionId: String? =
        savedStateHandle.get<String>("sessionId")?.takeIf { it != NEW }

    /** Optional prompt to prefill the composer (e.g. from a suggestion chip). */
    val initialInput: String = savedStateHandle.get<String>("prompt").orEmpty()

    private var currentStreamId: String? = null
    private var streamJob: Job? = null
    private var nextId = 0L

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun send(text: String) {
        val message = text.trim()
        if (message.isEmpty() || _state.value.running) return

        appendMessage(ChatMessage.Role.USER, message)
        val assistant = appendMessage(ChatMessage.Role.ASSISTANT, "")
        _state.update { it.copy(running = true, error = null) }

        streamJob = viewModelScope.launch {
            runCatching {
                when (val result = repository.startRun(sessionId, message)) {
                    is ChatRepository.StartResult.Cron -> {
                        val note = result.cron?.let { " (cron: $it)" }.orEmpty()
                        extendAssistant(assistant.id, "Added to your Scheduled tasks$note.")
                    }
                    is ChatRepository.StartResult.Run -> {
                        sessionId = result.sessionId ?: sessionId
                        currentStreamId = result.streamId
                        repository.stream(result.streamId).collect { reduce(it, assistant.id) }
                    }
                }
            }.onFailure { e ->
                _state.update { it.copy(running = false, error = e.message ?: "Stream failed") }
            }
            _state.update { it.copy(running = false) }
            currentStreamId = null
        }
    }

    fun cancel() {
        val id = currentStreamId
        streamJob?.cancel()
        _state.update { it.copy(running = false) }
        if (id != null) viewModelScope.launch { runCatching { repository.cancel(id) } }
    }

    private fun reduce(event: StreamEvent, assistantId: Long) {
        when (event) {
            is StreamEvent.Token -> {
                extendAssistant(assistantId, event.text)
                harvestArtifacts(assistantId)
            }
            is StreamEvent.Tool -> {
                addAction(ActionItem.Kind.TOOL, listOfNotNull(event.name, event.preview).joinToString(": "))
                _state.update { it.copy(toolsUsed = it.toolsUsed + event.name) }
            }
            is StreamEvent.Status -> addAction(ActionItem.Kind.STATUS, event.text)
            is StreamEvent.Reasoning -> addAction(ActionItem.Kind.REASONING, event.text)
            is StreamEvent.Approval ->
                addAction(ActionItem.Kind.STATUS, "Approval needed: ${event.command}")
            is StreamEvent.Clarify -> addAction(ActionItem.Kind.STATUS, "Asked: ${event.question}")
            is StreamEvent.Completed -> {
                event.raw?.let { harvestArtifactsFrom(it) }
                _state.update { it.copy(running = false) }
            }
            is StreamEvent.Error ->
                _state.update { it.copy(running = false, error = event.message) }
            StreamEvent.Interrupted -> _state.update { it.copy(running = false) }
            is StreamEvent.Unknown -> Unit
        }
    }

    private fun appendMessage(role: ChatMessage.Role, text: String): ChatMessage {
        val msg = ChatMessage(id = nextId++, role = role, text = text)
        _state.update { it.copy(messages = it.messages + msg) }
        return msg
    }

    private fun extendAssistant(id: Long, delta: String) = _state.update { s ->
        s.copy(messages = s.messages.map { if (it.id == id) it.copy(text = it.text + delta) else it })
    }

    private fun addAction(kind: ActionItem.Kind, label: String) {
        if (label.isBlank()) return
        _state.update { it.copy(actions = it.actions + ActionItem(nextId++, kind, label)) }
    }

    private fun harvestArtifacts(assistantId: Long) {
        val text = _state.value.messages.firstOrNull { it.id == assistantId }?.text ?: return
        harvestArtifactsFrom(text)
    }

    private fun harvestArtifactsFrom(text: String) {
        val found = Artifacts.extract(text)
        if (found.isEmpty()) return
        _state.update { s ->
            val merged = (s.artifacts + found).distinctBy { it.url }
            s.copy(artifacts = merged)
        }
    }

    companion object { const val NEW = "new" }
}
