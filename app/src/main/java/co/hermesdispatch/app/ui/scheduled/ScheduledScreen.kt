package co.hermesdispatch.app.ui.scheduled

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.hermesdispatch.app.domain.Schedule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledScreen(viewModel: ScheduledViewModel = hiltViewModel()) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(topBar = { TopAppBar(title = { Text("Scheduled") }) }) { padding ->
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
                            onTogglePause = { viewModel.togglePause(schedule) },
                            onRunNow = { viewModel.runNow(schedule) },
                            onDelete = { viewModel.delete(schedule) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleCard(
    schedule: Schedule,
    onTogglePause: () -> Unit,
    onRunNow: () -> Unit,
    onDelete: () -> Unit,
) {
    Card {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(schedule.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (schedule.paused) "Paused · ${schedule.cronExpr}" else schedule.cronExpr,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            IconButton(onClick = onRunNow) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Run now")
            }
            IconButton(onClick = onTogglePause) {
                Icon(
                    Icons.Filled.Pause,
                    contentDescription = if (schedule.paused) "Resume" else "Pause",
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}
