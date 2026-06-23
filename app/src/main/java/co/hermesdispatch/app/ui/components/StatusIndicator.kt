package co.hermesdispatch.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Run state a task card surfaces with its leading glyph. */
enum class RunState { ACTIVE, DONE, FAILED, SCHEDULED, IDLE }

/**
 * Map the bridge's task `status` to a [RunState]. The bridge sends
 * scheduled/active/done/failed/idle; the `contains` fallbacks tolerate a stale
 * Room cache from before the bridge update (e.g. legacy "cron"/source strings).
 */
fun runStateOf(status: String): RunState = when {
    status.equals("scheduled", true) || status.contains("cron", true) -> RunState.SCHEDULED
    status.equals("active", true) || status.contains("run", true) -> RunState.ACTIVE
    status.equals("failed", true) || status.contains("error", true) -> RunState.FAILED
    status.equals("done", true) || status.contains("complete", true) -> RunState.DONE
    else -> RunState.IDLE
}

/**
 * Leading status glyph for a task card: a rotating spinner while ACTIVE, a green
 * check when DONE, a red error glyph when FAILED, a schedule glyph for cron, and
 * the default "spark" otherwise. Replaces the static circle previously inlined in
 * TaskCard so status reads at a glance.
 */
@Composable
fun StatusIndicator(state: RunState, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    // (container, content) per state — leans on Material You roles for theming.
    val (container, content) = when (state) {
        RunState.DONE -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        RunState.FAILED -> scheme.errorContainer to scheme.onErrorContainer
        else -> scheme.primaryContainer to scheme.onPrimaryContainer
    }
    Box(
        modifier = modifier.size(40.dp).clip(CircleShape).background(container),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            RunState.ACTIVE -> Spinner(content)
            else -> Icon(
                imageVector = when (state) {
                    RunState.DONE -> Icons.Filled.CheckCircle
                    RunState.FAILED -> Icons.Filled.ErrorOutline
                    RunState.SCHEDULED -> Icons.Filled.Schedule
                    else -> Icons.Filled.AutoAwesome
                },
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun Spinner(tint: Color) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "angle",
    )
    Icon(
        Icons.Filled.Sync,
        contentDescription = "In progress",
        tint = tint,
        modifier = Modifier.size(22.dp).rotate(angle),
    )
}
