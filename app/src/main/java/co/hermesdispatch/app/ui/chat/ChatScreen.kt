package co.hermesdispatch.app.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.hermesdispatch.app.domain.ActionItem
import co.hermesdispatch.app.domain.Artifact
import co.hermesdispatch.app.domain.ChatMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var input by remember { mutableStateOf(viewModel.initialInput) }
    var attachedImage by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val onTranscript: (String) -> Unit = { transcript ->
        input = listOf(input.trim(), transcript.trim()).filter { it.isNotEmpty() }.joinToString(" ")
    }
    val deviceSpeech = rememberSpeechController(onTranscript)
    val serverSpeech = rememberServerSpeechController(onTranscript, viewModel::transcribe)
    val speech = if (viewModel.serverStt) serverSpeech else deviceSpeech

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview(),
    ) { bitmap -> if (bitmap != null) attachedImage = ImageUtil.bitmapToDataUrl(bitmap) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) attachedImage = ImageUtil.uriToDataUrl(context, uri) }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(state.messages.lastIndex) }
        }
    }

    fun doSend() {
        viewModel.send(input, listOfNotNull(attachedImage))
        input = ""
        attachedImage = null
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // The composer handles its own bottom inset (nav bar + keyboard) below.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title.ifBlank { "New task" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (viewModel.canRename) {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = { menuOpen = false; renaming = true },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        // navigationBarsPadding() then imePadding(): composer sits just above the
        // nav bar when the keyboard is closed, and just above the keyboard when open
        // (imePadding consumes the already-applied nav inset — no extra gap).
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .navigationBarsPadding().imePadding(),
        ) {
            if (state.running) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            if (state.artifacts.isNotEmpty()) ArtifactsStrip(state.artifacts)

            if (state.toolsUsed.isNotEmpty()) ToolsRow(state.toolsUsed)

            // Chat thread (primary). Bottom-aligned so a short conversation hugs
            // the composer instead of leaving a large empty gap below.
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
            ) {
                items(state.messages, key = { it.id }) { msg ->
                    MessageBubble(
                        message = msg,
                        onEdit = if (msg.role == ChatMessage.Role.USER) {
                            { editingMessage = msg.text }
                        } else {
                            null
                        },
                    )
                }
            }

            // Live "actions" pane (the split): what the agent is doing.
            if (state.actions.isNotEmpty()) {
                HorizontalDivider()
                ActionsPane(state.actions)
            }

            state.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            Composer(
                value = input,
                onValueChange = { input = it },
                running = state.running,
                voiceState = speech.state.value,
                voicePartial = speech.partial.value,
                hasImage = attachedImage != null,
                onMic = speech.start,
                onStopVoice = speech.stop,
                onCamera = { cameraLauncher.launch(null) },
                onGallery = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onRemoveImage = { attachedImage = null },
                onSend = ::doSend,
                onCancel = viewModel::cancel,
            )
        }
    }

    state.pendingApproval?.let { ap ->
        AlertDialog(
            onDismissRequest = { viewModel.approve("deny") },
            title = { Text("Approval needed") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (ap.description.isNotBlank()) Text(ap.description)
                    if (ap.command.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                ap.command,
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.approve("once") }) { Text("Allow once") } },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.approve("always") }) { Text("Always") }
                    TextButton(onClick = { viewModel.approve("deny") }) { Text("Deny") }
                }
            },
        )
    }

    state.pendingClarify?.let { question ->
        var answer by remember(question) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Agent needs input") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(question)
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.answerClarify(answer) },
                    enabled = answer.isNotBlank(),
                ) { Text("Send") }
            },
        )
    }

    if (renaming) {
        var name by remember { mutableStateOf(title) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Rename task") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.rename(name); renaming = false }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } },
        )
    }

    editingMessage?.let { original ->
        var edited by remember(original) { mutableStateOf(original) }
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text("Edit & resend") },
            text = {
                OutlinedTextField(
                    value = edited,
                    onValueChange = { edited = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.send(edited)
                        input = ""
                        editingMessage = null
                    },
                    enabled = edited.isNotBlank() && !state.running,
                ) { Text("Send") }
            },
            dismissButton = { TextButton(onClick = { editingMessage = null }) { Text("Cancel") } },
        )
    }
}

private data class ResultMeta(val label: String, val sub: String, val icon: ImageVector)

private fun resultMeta(artifact: Artifact): ResultMeta {
    val url = artifact.url
    val host = url.substringAfter("://").substringBefore('/')
    return when {
        "docs.google.com/spreadsheets" in url -> ResultMeta("Google Sheet", host, Icons.Filled.TableChart)
        "docs.google.com/document" in url -> ResultMeta("Google Doc", host, Icons.Filled.Description)
        "docs.google.com/presentation" in url -> ResultMeta("Slides", host, Icons.Filled.Slideshow)
        "docs.google.com/forms" in url || "forms.gle" in host -> ResultMeta("Form", host, Icons.Filled.Assignment)
        "drive.google.com" in url -> ResultMeta("Drive file", host, Icons.Filled.Folder)
        artifact.isImage -> ResultMeta("Image", host, Icons.Filled.Image)
        else -> ResultMeta(host.ifBlank { "Link" }, url.substringAfter(host).take(40), Icons.Filled.Link)
    }
}

@Composable
private fun ArtifactsStrip(artifacts: List<Artifact>) {
    val uriHandler = LocalUriHandler.current
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(artifacts, key = { it.url }) { artifact ->
            val meta = resultMeta(artifact)
            Card(
                onClick = { runCatching { uriHandler.openUri(artifact.url) } },
                modifier = Modifier.width(210.dp),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            meta.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(
                            meta.label,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (meta.sub.isNotBlank()) {
                            Text(
                                meta.sub,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(message: ChatMessage, onEdit: (() -> Unit)? = null) {
    val isUser = message.role == ChatMessage.Role.USER
    val bg = if (isUser) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val linkColor = MaterialTheme.colorScheme.primary
    val content = remember(message.text, linkColor) {
        linkify(message.text.ifEmpty { "…" }, linkColor)
    }
    val bubbleModifier = if (onEdit != null) {
        Modifier.fillMaxWidth(0.9f).combinedClickable(onClick = {}, onLongClick = onEdit)
    } else {
        Modifier.fillMaxWidth(0.9f)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(color = bg, shape = RoundedCornerShape(16.dp), modifier = bubbleModifier) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val thumb = remember(message.imageData) {
                    message.imageData?.let { ImageUtil.dataUrlToImageBitmap(it) }
                }
                if (thumb != null) {
                    Image(
                        bitmap = thumb,
                        contentDescription = "Attached image",
                        modifier = Modifier
                            .heightIn(max = 180.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                } else if (message.imageCount > 0) {
                    Text(
                        "🖼 ${message.imageCount} image${if (message.imageCount > 1) "s" else ""} attached",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                // Tappable links open in the relevant app / browser. User bubbles
                // skip text selection so a long-press edits the message instead.
                if (onEdit != null) {
                    Text(content, style = MaterialTheme.typography.bodyMedium)
                } else {
                    SelectionContainer {
                        Text(content, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private val URL_REGEX = Regex("""https?://[^\s<>"')\]]+""")

/** Turn URLs in [text] into tappable links; the platform opens them in the
 *  matching app (e.g. Google Sheets) or the browser. */
private fun linkify(text: String, linkColor: Color): AnnotatedString {
    val styles = TextLinkStyles(
        SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
    )
    return buildAnnotatedString {
        var last = 0
        for (m in URL_REGEX.findAll(text)) {
            append(text.substring(last, m.range.first))
            val raw = m.value
            val url = raw.trimEnd('.', ',', ';', ':', ')', ']')
            withLink(LinkAnnotation.Url(url, styles)) { append(url) }
            if (url.length < raw.length) append(raw.substring(url.length))
            last = m.range.last + 1
        }
        if (last < text.length) append(text.substring(last))
    }
}

@Composable
private fun ActionsPane(actions: List<ActionItem>) {
    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp).padding(8.dp)) {
        Text("Agent activity", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
        LazyColumn(reverseLayout = true) {
            items(actions.asReversed(), key = { it.id }) { action ->
                Text(
                    "• ${action.label}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun ToolsRow(tools: Set<String>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(tools.toList(), key = { it }) { tool ->
            AssistChip(onClick = {}, label = { Text(tool, maxLines = 1) })
        }
    }
}

@Composable
private fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    running: Boolean,
    voiceState: VoiceState,
    voicePartial: String,
    hasImage: Boolean,
    onMic: () -> Unit,
    onStopVoice: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRemoveImage: () -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
) {
    val listening = voiceState == VoiceState.LISTENING || voiceState == VoiceState.TRANSCRIBING
    val display = when (voiceState) {
        VoiceState.TRANSCRIBING -> voicePartial.ifBlank { "Transcribing…" }
        VoiceState.LISTENING -> voicePartial.ifBlank { "Listening…" }
        else -> value
    }
    val canSend = (value.isNotBlank() || hasImage) && !listening
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        if (hasImage) {
            InputChip(
                selected = true,
                onClick = onRemoveImage,
                label = { Text("Image attached") },
                trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove image") },
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCamera, enabled = !running && !listening) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = "Take photo")
            }
            IconButton(onClick = onGallery, enabled = !running && !listening) {
                Icon(Icons.Filled.Image, contentDescription = "Attach image")
            }
            if (voiceState != VoiceState.UNAVAILABLE) {
                val recording = voiceState == VoiceState.LISTENING
                IconButton(
                    onClick = { if (recording) onStopVoice() else onMic() },
                    enabled = !running && voiceState != VoiceState.TRANSCRIBING,
                ) {
                    Icon(
                        if (recording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (recording) "Stop recording" else "Voice input",
                    )
                }
            }
            OutlinedTextField(
                value = display,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = !listening,
                placeholder = { Text("Message your agent…") },
                maxLines = 4,
            )
            Box(modifier = Modifier.padding(start = 8.dp)) {
                if (running) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop")
                    }
                } else {
                    IconButton(onClick = onSend, enabled = canSend) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}
