package co.hermesdispatch.app.ui.scheduled

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.hermesdispatch.app.domain.Schedule
import co.hermesdispatch.app.ui.components.TitleWithProfile
import co.hermesdispatch.app.ui.components.rememberHaptics
import co.hermesdispatch.app.ui.util.CronText
import co.hermesdispatch.app.ui.util.TimeFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledScreen(viewModel: ScheduledViewModel = hiltViewModel()) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val alerting by viewModel.alerting.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Schedule?>(null) }

    // Refresh on resume (first show, tab-return, app foreground) — a restored tab
    // composable won't re-run LaunchedEffect(Unit).
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = { TopAppBar(title = { TitleWithProfile("Scheduled", viewModel.activeProfile) }) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            if (schedules.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "No scheduled tasks. Recurring jobs your agent classifies as cron will appear here.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(schedules, key = { it.id }) { schedule ->
                        ScheduleCard(
                            schedule = schedule,
                            alerting = schedule.id in alerting,
                            onToggleAlert = { viewModel.toggleAlert(schedule) },
                            onTogglePause = { viewModel.togglePause(schedule) },
                            onRunNow = { viewModel.runNow(schedule) },
                            onEdit = { editing = schedule },
                            onDelete = { viewModel.delete(schedule) },
                        )
                    }
                }
            }
        }
    }

    editing?.let { schedule ->
        EditScheduleDialog(
            schedule = schedule,
            onDismiss = { editing = null },
            onSave = { name, prompt, cron ->
                viewModel.update(schedule, name, prompt, cron)
                editing = null
            },
        )
    }
}

@Composable
private fun EditScheduleDialog(
    schedule: Schedule,
    onDismiss: () -> Unit,
    onSave: (name: String, prompt: String, cron: String) -> Unit,
) {
    var name by remember { mutableStateOf(schedule.name) }
    var prompt by remember { mutableStateOf(schedule.prompt) }
    var cron by remember { mutableStateOf(schedule.cronExpr) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit scheduled task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Prompt") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = cron,
                    onValueChange = { cron = it },
                    label = { Text("Schedule") },
                    placeholder = { Text("e.g. every 10m or 0 9 * * *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, prompt, cron) },
                enabled = name.isNotBlank() || prompt.isNotBlank() || cron.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ScheduleCard(
    schedule: Schedule,
    alerting: Boolean,
    onToggleAlert: () -> Unit,
    onTogglePause: () -> Unit,
    onRunNow: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()
    val tint = if (schedule.paused) MaterialTheme.colorScheme.surfaceVariant
    else MaterialTheme.colorScheme.primaryContainer
    val next = schedule.nextRun?.let { TimeFormat.relative(it) }.orEmpty()
    val subtitle = listOfNotNull(
        CronText.humanize(schedule.cronExpr).ifBlank { null },
        if (schedule.paused) "Paused" else next.ifBlank { null }?.let { "Next $it" },
    ).joinToString(" · ")
    Card {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(tint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    schedule.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
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
            // Inline pause/resume with an animated icon crossfade + haptic.
            IconButton(onClick = { haptics.tick(); onTogglePause() }) {
                Crossfade(targetState = schedule.paused, label = "pause") { paused ->
                    Icon(
                        if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (paused) "Resume" else "Pause",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Alert bell (per cron job — one row per job, so no ambiguity).
            IconButton(onClick = onToggleAlert) {
                Icon(
                    if (alerting) Icons.Filled.NotificationsActive else Icons.Outlined.Notifications,
                    contentDescription = if (alerting) "Alerts on" else "Alerts off",
                    tint = if (alerting) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Run now") },
                        leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                        onClick = { menuOpen = false; onRunNow() },
                    )
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = { menuOpen = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text(if (schedule.paused) "Resume" else "Pause") },
                        leadingIcon = {
                            Icon(
                                if (schedule.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = null,
                            )
                        },
                        onClick = { menuOpen = false; onTogglePause() },
                    )
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
