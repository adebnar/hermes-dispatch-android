package co.hermesdispatch.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.hermesdispatch.app.domain.Task
import co.hermesdispatch.app.ui.components.SegmentedControl
import co.hermesdispatch.app.ui.components.StatusIndicator
import co.hermesdispatch.app.ui.components.TitleWithProfile
import co.hermesdispatch.app.ui.components.runStateOf
import co.hermesdispatch.app.ui.util.TimeFormat
import kotlinx.coroutines.launch

private val FILTER_LABELS = listOf("All", "Scheduled", "Completed")

private val SUGGESTIONS = listOf(
    "Summarize my unread email and draft replies",
    "Create a weekly digest of my top priorities",
    "Research and compare options in a spreadsheet",
    "Draft and schedule a follow-up message",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onTaskClick: (String, String) -> Unit,
    onNewTask: (String?) -> Unit,
    onQuickAction: (String) -> Unit = {},
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val visibleTasks by viewModel.visibleTasks.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val showArchived by viewModel.showArchived.collectAsStateWithLifecycle()
    val archived by viewModel.archived.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.refresh() }

    fun archiveWithUndo(task: Task) {
        viewModel.archive(task)
        scope.launch {
            if (snackbar.showSnackbar("Archived", "Undo") == SnackbarResult.ActionPerformed) {
                viewModel.undo()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { TitleWithProfile("Tasks", viewModel.activeProfile) },
                actions = {
                    IconButton(onClick = { viewModel.setShowArchived(!showArchived) }) {
                        Icon(
                            if (showArchived) Icons.Filled.Inventory2 else Icons.Filled.Archive,
                            contentDescription = if (showArchived) "Show active" else "Show archived",
                            tint = if (showArchived) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNewTask(null) }) {
                Icon(Icons.Filled.Add, contentDescription = "New task")
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search sessions") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
            )

            if (query.isBlank() && !showArchived) {
                QuickActionBar(onQuickAction)
                SegmentedControl(
                    options = FILTER_LABELS,
                    selectedIndex = filter.ordinal,
                    onSelect = { viewModel.setFilter(TaskFilter.entries[it]) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    query.isNotBlank() -> FlatList(
                        items = results,
                        empty = "No sessions match \"$query\".",
                        onTaskClick = onTaskClick,
                    )
                    showArchived -> FlatList(
                        items = archived,
                        empty = "No archived tasks.",
                        onTaskClick = onTaskClick,
                        swipe = SwipeAction(Icons.Filled.Unarchive, "Unarchive") { viewModel.unarchive(it) },
                    )
                    else -> ActiveList(
                        tasks = visibleTasks,
                        showSuggestions = filter == TaskFilter.ALL,
                        emptyMessage = when (filter) {
                            TaskFilter.ALL -> "No tasks yet. Tap + or pick a suggestion to put your agent to work."
                            TaskFilter.SCHEDULED -> "No scheduled tasks. Recurring jobs show up here."
                            TaskFilter.COMPLETED -> "No completed tasks yet."
                        },
                        error = error,
                        onRetry = viewModel::refresh,
                        onNewTask = onNewTask,
                        onTaskClick = onTaskClick,
                        onArchive = ::archiveWithUndo,
                    )
                }
            }
        }
    }
}

private class SwipeAction(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val onSwipe: (Task) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlatList(
    items: List<Task>,
    empty: String,
    onTaskClick: (String, String) -> Unit,
    swipe: SwipeAction? = null,
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Text(empty, style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = { it.id }) { task ->
            if (swipe != null) {
                SwipeCard(task, swipe) { TaskCard(task) { onTaskClick(task.id, task.title) } }
            } else {
                TaskCard(task) { onTaskClick(task.id, task.title) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveList(
    tasks: List<Task>,
    showSuggestions: Boolean,
    emptyMessage: String,
    error: String?,
    onRetry: () -> Unit,
    onNewTask: (String?) -> Unit,
    onTaskClick: (String, String) -> Unit,
    onArchive: (Task) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showSuggestions) item { SuggestionsRow(onPick = { onNewTask(it) }) }

        error?.let { item { ErrorBanner(message = it, onRetry = onRetry) } }

        if (tasks.isEmpty()) {
            item {
                Text(
                    emptyMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        } else {
            val grouped = tasks.groupBy { TimeFormat.bucket(it.updatedAt) }
            grouped.forEach { (label, group) ->
                item(key = "header-$label") { SectionHeader(label) }
                items(group, key = { it.id }) { task ->
                    SwipeCard(
                        task,
                        SwipeAction(Icons.Filled.Archive, "Archive") { onArchive(task) },
                    ) { TaskCard(task) { onTaskClick(task.id, task.title) } }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeCard(task: Task, action: SwipeAction, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it != SwipeToDismissBoxValue.Settled) { action.onSwipe(task); true } else false
        },
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    action.icon,
                    contentDescription = action.label,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        },
    ) { content() }
}

@Composable
private fun QuickActionBar(onQuickAction: (String) -> Unit) {
    val actions = listOf(
        Triple("voice", Icons.Filled.Mic, "Voice task"),
        Triple("gallery", Icons.Filled.Image, "Image task"),
        Triple("document", Icons.Filled.AttachFile, "Document task"),
        Triple("clipboard", Icons.Filled.ContentPaste, "Paste task"),
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        actions.forEach { (mode, icon, desc) ->
            FilledTonalIconButton(
                onClick = { onQuickAction(mode) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(icon, contentDescription = desc)
            }
        }
    }
}

@Composable
private fun SuggestionsRow(onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Suggested",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(SUGGESTIONS, key = { it }) { suggestion ->
                ElevatedCard(
                    onClick = { onPick(suggestion) },
                    modifier = Modifier
                        .width(180.dp)
                        .height(108.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(12.dp),
                        ),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            suggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                "Couldn't reach your Hermes bridge.",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
            )
            Text(message, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun TaskCard(task: Task, onClick: () -> Unit) {
    val state = runStateOf(task.status)
    val label = when (state) {
        co.hermesdispatch.app.ui.components.RunState.SCHEDULED -> "Scheduled"
        co.hermesdispatch.app.ui.components.RunState.ACTIVE -> "Running"
        co.hermesdispatch.app.ui.components.RunState.FAILED -> "Failed"
        else -> ""
    }
    val when_ = TimeFormat.relative(task.updatedAt)
    val subtitle = listOf(label, when_).filter { it.isNotBlank() }.joinToString(" · ")
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusIndicator(state)
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
