package co.hermesdispatch.app.ui.chat

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

enum class VoiceState { IDLE, LISTENING, TRANSCRIBING, UNAVAILABLE }

/** Imperative handle returned to the UI: current [state], live [partial] text, and [start]. */
class SpeechController internal constructor(
    val state: State<VoiceState>,
    val partial: State<String>,
    val start: () -> Unit,
)

/**
 * On-device speech-to-text via [SpeechRecognizer] with offline preference, plus
 * RECORD_AUDIO permission handling. Final transcripts are delivered to
 * [onFinalText]; partials stream through [SpeechController.partial] so the UI can
 * show a live "Transcribing…" state. Recognition stays on-device when a local
 * model is available, matching the app's no-cloud-by-default posture.
 */
@Composable
fun rememberSpeechController(onFinalText: (String) -> Unit): SpeechController {
    val context = LocalContext.current
    val available = remember { SpeechRecognizer.isRecognitionAvailable(context) }
    val state = remember { mutableStateOf(if (available) VoiceState.IDLE else VoiceState.UNAVAILABLE) }
    val partial = remember { mutableStateOf("") }

    val recognizer = remember {
        if (available) SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    val intent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
    }

    fun beginListening() {
        val r = recognizer ?: return
        partial.value = ""
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { state.value = VoiceState.LISTENING }
            override fun onBeginningOfSpeech() { state.value = VoiceState.LISTENING }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { state.value = VoiceState.TRANSCRIBING }
            override fun onError(error: Int) {
                state.value = VoiceState.IDLE
                partial.value = ""
            }
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.firstResult()?.let { partial.value = it }
            }
            override fun onResults(results: Bundle?) {
                val text = results?.firstResult().orEmpty()
                partial.value = ""
                state.value = VoiceState.IDLE
                if (text.isNotBlank()) onFinalText(text)
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        r.startListening(intent)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) beginListening() else state.value = VoiceState.IDLE }

    DisposableEffect(Unit) {
        onDispose { recognizer?.destroy() }
    }

    val start: () -> Unit = start@{
        if (recognizer == null) return@start
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    return remember { SpeechController(state, partial, start) }
}

private fun Bundle.firstResult(): String? =
    getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
