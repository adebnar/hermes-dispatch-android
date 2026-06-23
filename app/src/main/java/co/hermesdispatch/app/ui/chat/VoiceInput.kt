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
import kotlinx.coroutines.launch

enum class VoiceState { IDLE, LISTENING, TRANSCRIBING, UNAVAILABLE }

/** Imperative handle returned to the UI: [state], live [partial] text, [start], [stop].
 *  [amplitude] is a normalized 0..1 mic level for the recording visualizer. */
class SpeechController internal constructor(
    val state: State<VoiceState>,
    val partial: State<String>,
    val start: () -> Unit,
    val stop: () -> Unit = {},
    val amplitude: State<Float>,
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
    val amplitude = remember { mutableStateOf(0f) }

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
            override fun onRmsChanged(rmsdB: Float) {
                // RMS is roughly -2..10 dB; map to 0..1 for the visualizer.
                amplitude.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { state.value = VoiceState.TRANSCRIBING; amplitude.value = 0f }
            override fun onError(error: Int) {
                state.value = VoiceState.IDLE
                partial.value = ""
                amplitude.value = 0f
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

    val stop: () -> Unit = { recognizer?.stopListening() }

    return remember { SpeechController(state, partial, start, stop, amplitude) }
}

private fun Bundle.firstResult(): String? =
    getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

/**
 * Server-side speech-to-text: records audio with [android.media.MediaRecorder]
 * to a temp m4a, then [transcribe]s it via the bridge. Same [SpeechController]
 * shape as the on-device path so the composer can swap between them. [start]
 * begins recording; [stop] ends it and uploads.
 */
@Composable
fun rememberServerSpeechController(
    onFinalText: (String) -> Unit,
    transcribe: suspend (ByteArray) -> String,
): SpeechController {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val state = remember { mutableStateOf(VoiceState.IDLE) }
    val partial = remember { mutableStateOf("") }
    val amplitude = remember { mutableStateOf(0f) }
    val recorder = remember { mutableStateOf<android.media.MediaRecorder?>(null) }
    val outFile = remember { mutableStateOf<java.io.File?>(null) }

    fun beginRecording() {
        runCatching {
            val file = java.io.File.createTempFile("hd-stt-", ".m4a", context.cacheDir)
            val rec = if (android.os.Build.VERSION.SDK_INT >= 31) {
                android.media.MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                android.media.MediaRecorder()
            }
            rec.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
            rec.setAudioEncodingBitRate(64_000)
            rec.setAudioSamplingRate(16_000)
            rec.setOutputFile(file.absolutePath)
            rec.prepare()
            rec.start()
            recorder.value = rec
            outFile.value = file
            state.value = VoiceState.LISTENING
            // Poll the recorder's peak amplitude to drive the visualizer.
            scope.launch {
                while (recorder.value != null && state.value == VoiceState.LISTENING) {
                    val peak = runCatching { recorder.value?.maxAmplitude ?: 0 }.getOrDefault(0)
                    amplitude.value = (peak / 32767f).coerceIn(0f, 1f)
                    kotlinx.coroutines.delay(80)
                }
                amplitude.value = 0f
            }
        }.onFailure { state.value = VoiceState.IDLE }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) beginRecording() else state.value = VoiceState.IDLE }

    val start: () -> Unit = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }

    val stop: () -> Unit = stop@{
        val rec = recorder.value ?: return@stop
        val file = outFile.value
        state.value = VoiceState.TRANSCRIBING
        runCatching { rec.stop() }
        runCatching { rec.release() }
        recorder.value = null
        scope.launch {
            val bytes = runCatching { file?.readBytes() }.getOrNull()
            val text = if (bytes != null && bytes.isNotEmpty()) {
                runCatching { transcribe(bytes) }.getOrDefault("")
            } else {
                ""
            }
            runCatching { file?.delete() }
            partial.value = ""
            state.value = VoiceState.IDLE
            if (text.isNotBlank()) onFinalText(text)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { recorder.value?.release() }
            recorder.value = null
        }
    }

    return remember { SpeechController(state, partial, start, stop, amplitude) }
}
