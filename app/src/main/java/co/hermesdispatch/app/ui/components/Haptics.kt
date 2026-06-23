package co.hermesdispatch.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Small wrapper over [LocalHapticFeedback] so call sites read intent, not the
 * raw [HapticFeedbackType] constants. Use for confirming key interactions
 * (starting voice capture, pausing a schedule, a task completing).
 */
class Haptics(private val feedback: HapticFeedback) {
    /** Light feedback for a discrete toggle/selection (pause, segment switch). */
    fun tick() = feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)

    /** Stronger feedback for a committed action (start recording, task done). */
    fun confirm() = feedback.performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun rememberHaptics(): Haptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { Haptics(feedback) }
}
