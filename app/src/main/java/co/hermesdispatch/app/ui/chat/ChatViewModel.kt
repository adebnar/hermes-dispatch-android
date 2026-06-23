package co.hermesdispatch.app.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.hermesdispatch.app.data.prefs.SecureSettings
import co.hermesdispatch.app.data.remote.sse.StreamEvent
import co.hermesdispatch.app.data.repository.ChatRepository
import co.hermesdispatch.app.data.repository.TaskRepository
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class ApprovalRequest(val command: String, val description: String)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val actions: List<ActionItem> = emptyList(),
    val artifacts: List<Artifact> = emptyList(),
    val toolsUsed: Set<String> = emptySet(),
    val pendingApproval: ApprovalRequest? = null,
    val pendingClarify: String? = null,
    val running: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val tasks: TaskRepository,
    private val settings: SecureSettings,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Whether to use the bridge's server-side transcription instead of on-device. */
    val serverStt: Boolean get() = settings.serverTranscription()

    /** Upload recorded audio for server-side transcription. */
    suspend fun transcribe(audio: ByteArray): String =
        runCatching { repository.transcribe(audio) }.getOrDefault("")

    // "new" => no session yet; the server creates one on first send.
    private var sessionId: String? =
        savedStateHandle.get<String>("sessionId")?.takeIf { it != NEW }

    /** Optional prompt to prefill the composer (e.g. from a suggestion chip). */
    val initialInput: String = savedStateHandle.get<String>("prompt").orEmpty()

    /** Task title for the header when opening an existing task. */
    val initialTitle: String = savedStateHandle.get<String>("title").orEmpty()

    /** Header title — starts from the nav arg, follows any local rename live. */
    private val _title = MutableStateFlow(initialTitle)
    val title: StateFlow<String> = _title.asStateFlow()

    init {
        // Opening an existing task: load its conversation history + any rename.
        val sid = sessionId
        if (sid != null) {
            viewModelScope.launch {
                runCatching { repository.history(sid) }.getOrNull()?.forEach { (role, text) ->
                    appendMessage(role, text)
                }
            }
            viewModelScope.launch {
                tasks.observeLabel(sid).collect { label ->
                    _title.value = label?.ifBlank { null } ?: initialTitle
                }
            }
        }
    }

    /** Give this task a custom display name (persisted locally). */
    fun rename(text: String) {
        val sid = sessionId ?: return
        _title.value = text.trim().ifBlank { initialTitle }  // optimistic
        viewModelScope.launch { tasks.setLabel(sid, text) }
    }

    /** Rename is only meaningful once the task has a session id. */
    val canRename: Boolean get() = sessionId != null

    /** Models available for the per-session model picker. */
    suspend fun modelOptions(): List<co.hermesdispatch.app.data.remote.dto.ModelOptionDto> =
        runCatching { repository.models() }.getOrDefault(emptyList())

    /**
     * Hot-swap the model for this session by sending the agent's `/model`
     * slash command (the dashboard supports per-session model switching this
     * way; it applies to this conversation only, not the profile default).
     */
    fun changeModel(provider: String, model: String) {
        send("/model $model --provider $provider")
    }

    private var currentStreamId: String? = null
    private var streamJob: Job? = null
    private var nextId = 0L

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun send(text: String, images: List<String> = emptyList()) {
        val message = text.trim()
        if ((message.isEmpty() && images.isEmpty()) || _state.value.running) return

        appendMessage(
            ChatMessage.Role.USER,
            message.ifEmpty { "(image)" },
            imageCount = images.size,
            imageData = images.firstOrNull(),
        )
        val assistant = appendMessage(ChatMessage.Role.ASSISTANT, "")
        _state.update { it.copy(running = true, error = null) }

        streamJob = viewModelScope.launch {
            runCatching {
                when (val result = repository.startRun(sessionId, message, images)) {
                    is ChatRepository.StartResult.Cron -> {
                        val note = result.cron?.let { " (cron: $it)" }.orEmpty()
                        extendAssistant(assistant.id, "Added to your Scheduled tasks$note.")
                    }
                    is ChatRepository.StartResult.Run -> {
                        sessionId = result.sessionId ?: sessionId
                        currentStreamId = result.streamId
                        streamWithRetry(result.streamId, assistant.id)
                    }
                }
            }.onFailure { e ->
                if (e !is kotlinx.coroutines.CancellationException) {
                    _state.update { it.copy(running = false, error = e.message ?: "Stream failed") }
                }
            }
            _state.update { it.copy(running = false) }
            currentStreamId = null
        }
    }

    /**
     * Collect the run's SSE stream, resilient to transient socket drops
     * ("Software caused connection abort") on long local-model turns. The bridge
     * buffers + replays each run's frames, so on a drop we resubscribe (replaying
     * from the start — we reset the assistant bubble so deltas don't double). If
     * the run already finished, a post-completion drop is ignored; if retries are
     * exhausted, we reconcile from server history instead of showing an error.
     */
    private suspend fun streamWithRetry(streamId: String, assistantId: Long) {
        var attempt = 0
        var terminal = false
        while (true) {
            try {
                if (attempt > 0) setAssistantText(assistantId, "") // replay rebuilds from start
                repository.stream(streamId).collect { ev ->
                    if (ev is StreamEvent.Completed || ev is StreamEvent.Error ||
                        ev is StreamEvent.Interrupted
                    ) {
                        terminal = true
                    }
                    reduce(ev, assistantId)
                }
                return // stream ended cleanly (server sent its terminal sentinel)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (terminal) return // run finished; ignore a post-completion abort
                if (attempt >= MAX_STREAM_RETRIES) {
                    // The run is likely still finishing server-side; reconcile from
                    // history rather than surfacing a transient socket error.
                    reconcileFromHistory()
                    return
                }
                attempt++
                kotlinx.coroutines.delay(500L * attempt)
            }
        }
    }

    /** Replace the in-memory thread with canonical server history (post-drop). */
    private suspend fun reconcileFromHistory() {
        val sid = sessionId ?: return
        val history = runCatching { repository.history(sid) }.getOrNull() ?: return
        if (history.isEmpty()) return
        _state.update { s ->
            s.copy(
                messages = history.mapIndexed { i, (role, text) ->
                    ChatMessage(id = -(i.toLong()) - 1, role = role, text = text)
                },
                error = null,
            )
        }
    }

    fun approve(choice: String) {
        val id = currentStreamId ?: return
        _state.update { it.copy(pendingApproval = null) }
        viewModelScope.launch { runCatching { repository.approve(id, choice) } }
    }

    fun answerClarify(text: String) {
        val id = currentStreamId ?: return
        _state.update { it.copy(pendingClarify = null) }
        viewModelScope.launch { runCatching { repository.clarify(id, text) } }
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
            is StreamEvent.Approval -> {
                addAction(ActionItem.Kind.STATUS, "Approval needed: ${event.command}")
                _state.update {
                    it.copy(pendingApproval = ApprovalRequest(event.command, event.description ?: ""))
                }
            }
            is StreamEvent.Clarify -> {
                addAction(ActionItem.Kind.STATUS, "Asked: ${event.question}")
                _state.update { it.copy(pendingClarify = event.question) }
            }
            is StreamEvent.Completed -> {
                event.raw?.let { raw ->
                    harvestArtifactsFrom(raw)
                    // Some models return the answer only in the completion event
                    // (no token deltas) — fill the bubble so it isn't left blank.
                    val finalText = completedText(raw)
                    if (finalText.isNotBlank()) setAssistantIfEmpty(assistantId, finalText)
                }
                _state.update { it.copy(running = false) }
            }
            is StreamEvent.Error ->
                _state.update { it.copy(running = false, error = event.message) }
            StreamEvent.Interrupted -> _state.update { it.copy(running = false) }
            is StreamEvent.Unknown -> Unit
        }
    }

    private fun appendMessage(
        role: ChatMessage.Role,
        text: String,
        imageCount: Int = 0,
        imageData: String? = null,
    ): ChatMessage {
        val msg = ChatMessage(
            id = nextId++, role = role, text = text,
            imageCount = imageCount, imageData = imageData,
        )
        _state.update { it.copy(messages = it.messages + msg) }
        return msg
    }

    private fun extendAssistant(id: Long, delta: String) = _state.update { s ->
        s.copy(messages = s.messages.map { if (it.id == id) it.copy(text = it.text + delta) else it })
    }

    private fun setAssistantText(id: Long, text: String) = _state.update { s ->
        s.copy(messages = s.messages.map { if (it.id == id) it.copy(text = text) else it })
    }

    private fun setAssistantIfEmpty(id: Long, text: String) = _state.update { s ->
        s.copy(messages = s.messages.map {
            if (it.id == id && it.text.isBlank()) it.copy(text = text) else it
        })
    }

    private fun completedText(raw: String): String = runCatching {
        (completedJson.parseToJsonElement(raw) as? JsonObject)
            ?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty()
    }.getOrDefault("")

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

    companion object {
        const val NEW = "new"
        private const val MAX_STREAM_RETRIES = 3
        private val completedJson = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}
