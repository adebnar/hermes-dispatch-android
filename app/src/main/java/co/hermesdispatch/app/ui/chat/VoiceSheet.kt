package co.hermesdispatch.app.ui.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Voice-capture bottom sheet with a live amplitude waveform. Auto-starts the
 * given [speech] controller on open, shows the partial transcript, and closes
 * itself once recognition finishes (the transcript is delivered through the
 * controller's onFinalText, which fills the composer).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSheet(speech: SpeechController, onClose: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val voice by speech.state
    val partial by speech.partial
    val amp by speech.amplitude

    LaunchedEffect(Unit) { speech.start() }

    // Close once we return to IDLE after having been active (final delivered).
    var wasActive by remember { mutableStateOf(false) }
    LaunchedEffect(voice) {
        when (voice) {
            VoiceState.LISTENING, VoiceState.TRANSCRIBING -> wasActive = true
            VoiceState.IDLE -> if (wasActive) onClose()
            VoiceState.UNAVAILABLE -> onClose()
        }
    }

    ModalBottomSheet(
        onDismissRequest = { speech.stop(); onClose() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                when (voice) {
                    VoiceState.TRANSCRIBING -> "Transcribing…"
                    VoiceState.LISTENING -> "Listening…"
                    else -> "Getting ready…"
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Waveform(amplitude = amp, active = voice == VoiceState.LISTENING)
            Text(
                partial.ifBlank { "Speak your task — it'll fill the message box." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { speech.stop() }, enabled = voice == VoiceState.LISTENING) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun Waveform(amplitude: Float, active: Boolean) {
    val bars = 28
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val color = MaterialTheme.colorScheme.primary
    // A small idle shimmer so the bar row never looks dead, scaled up by the
    // live mic amplitude while listening.
    val level = if (active) amplitude.coerceIn(0f, 1f) else 0f
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(bars) { i ->
            val wave = 0.5f + 0.5f * sin(phase + i * 0.5f)
            val frac = (0.08f + (0.15f + 0.85f * level) * wave).coerceIn(0.05f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(frac)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}
