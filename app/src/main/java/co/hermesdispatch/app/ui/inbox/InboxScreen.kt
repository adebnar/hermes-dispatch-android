package co.hermesdispatch.app.ui.inbox

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.hermesdispatch.app.domain.InboxItem
import co.hermesdispatch.app.ui.components.TitleWithProfile
import co.hermesdispatch.app.ui.util.TimeFormat
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onOpen: (id: String, title: String) -> Unit,
    viewModel: InboxViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val showArchived by viewModel.showArchived.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { TitleWithProfile("Inbox", viewModel.activeProfile) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !showArchived,
                    onClick = { viewModel.setShowArchived(false) },
                    label = { Text("Inbox") },
                )
                FilterChip(
                    selected = showArchived,
                    onClick = { viewModel.setShowArchived(true) },
                    label = { Text("Archived") },
                )
            }
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (items.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            error ?: if (showArchived) {
                                "No archived items."
                            } else {
                                "Nothing delivered locally yet. Cron jobs that save results to " +
                                    "this desktop (deliver = local) show up here."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(items, key = { it.id }) { item ->
                            InboxRow(
                                item = item,
                                archivedView = showArchived,
                                onOpen = {
                                    viewModel.markRead(item.id)
                                    onOpen(item.id, item.display)
                                },
                                onArchive = {
                                    viewModel.archive(item)
                                    scope.launch {
                                        val r = snackbar.showSnackbar("Archived", "Undo")
                                        if (r == SnackbarResult.ActionPerformed) viewModel.undo()
                                    }
                                },
                                onDelete = {
                                    viewModel.delete(item)
                                    scope.launch {
                                        val r = snackbar.showSnackbar("Removed", "Undo")
                                        if (r == SnackbarResult.ActionPerformed) viewModel.undo()
                                    }
                                },
                                onRestore = { viewModel.restore(item) },
                                onTogglePin = { viewModel.togglePin(item) },
                                onToggleAlert = { viewModel.toggleAlert(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun InboxRow(
    item: InboxItem,
    archivedView: Boolean,
    onOpen: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleAlert: () -> Unit,
) {
    if (archivedView) {
        InboxCard(item, onOpen, onDelete, onRestore, onTogglePin, onToggleAlert, archivedView)
        return
    }
    // Active view: swipe to archive (either direction).
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) { onArchive(); true } else false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    Icons.Filled.Archive,
                    contentDescription = "Archive",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        },
    ) {
        InboxCard(item, onOpen, onDelete, onRestore, onTogglePin, onToggleAlert, archivedView)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InboxCard(
    item: InboxItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleAlert: () -> Unit,
    archivedView: Boolean,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val failed = item.status == "failed"
    val tint = if (failed) MaterialTheme.colorScheme.errorContainer
    else MaterialTheme.colorScheme.primaryContainer
    val onTint = if (failed) MaterialTheme.colorScheme.onErrorContainer
    else MaterialTheme.colorScheme.onPrimaryContainer
    val when_ = item.createdAt?.let { TimeFormat.relative(it) }.orEmpty()

    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = onOpen,
            onLongClick = { menuOpen = true },
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(tint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (failed) Icons.Filled.ErrorOutline else Icons.Filled.Description,
                    contentDescription = null,
                    tint = onTint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.unread) {
                        Box(
                            modifier = Modifier.size(8.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                        Box(Modifier.size(6.dp))
                    }
                    Text(
                        item.display,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (item.unread) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val subtitle = listOf(
                    if (failed) "Failed" else "",
                    item.jobName.takeIf { it != item.display }.orEmpty(),
                    when_,
                ).filter { it.isNotBlank() }.joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (item.snippet.isNotBlank()) {
                    Text(
                        item.snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            // Passive status glyphs (not tap targets — alert/pin are in the menu).
            if (item.alerting) {
                Icon(
                    Icons.Filled.NotificationsActive,
                    contentDescription = "This job alerts you",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp).padding(end = 4.dp),
                )
            }
            if (item.pinned) {
                Icon(
                    Icons.Filled.PushPin,
                    contentDescription = "Pinned",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp).padding(end = 8.dp),
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(if (item.alerting) "Stop alerting this job" else "Alert me about this job") },
                        leadingIcon = {
                            Icon(
                                if (item.alerting) Icons.Outlined.Notifications
                                else Icons.Filled.NotificationsActive,
                                contentDescription = null,
                            )
                        },
                        onClick = { menuOpen = false; onToggleAlert() },
                    )
                    DropdownMenuItem(
                        text = { Text(if (item.pinned) "Unpin" else "Pin") },
                        leadingIcon = {
                            Icon(
                                if (item.pinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                                contentDescription = null,
                            )
                        },
                        onClick = { menuOpen = false; onTogglePin() },
                    )
                    if (archivedView) {
                        DropdownMenuItem(
                            text = { Text("Restore") },
                            leadingIcon = { Icon(Icons.Filled.Unarchive, contentDescription = null) },
                            onClick = { menuOpen = false; onRestore() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}
