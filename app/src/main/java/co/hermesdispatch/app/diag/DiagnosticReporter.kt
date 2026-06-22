package co.hermesdispatch.app.diag

import android.content.Context
import android.os.Build
import androidx.core.content.FileProvider
import co.hermesdispatch.app.BuildConfig
import java.io.File

/**
 * Builds an opt-in diagnostic report: device/app info plus this app's own recent
 * logs. Secrets are redacted twice over — known stored values are masked by exact
 * match, and credential-shaped patterns are scrubbed — but redaction is
 * best-effort, so the user is always shown the result before sharing.
 */
object DiagnosticReporter {

    private const val MAX_LOG_LINES = 600
    const val REDACTED = "***REDACTED***"

    /** Build the redacted report. [maskValues] are exact secrets to strip (token, key). */
    fun build(context: Context, maskValues: List<String>): String {
        val sb = StringBuilder()
        sb.appendLine("Hermes Dispatch — diagnostic report")
        sb.appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) ${BuildConfig.BUILD_TYPE}")
        sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        sb.appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        sb.appendLine("Uptime ms: ${android.os.SystemClock.elapsedRealtime()}")
        sb.appendLine()
        sb.appendLine("---- recent app logs ----")
        sb.append(readOwnLogcat())
        return redact(sb.toString(), maskValues)
    }

    /** Read this process's own logcat (apps can only read their own on modern Android). */
    private fun readOwnLogcat(): String = runCatching {
        val pid = android.os.Process.myPid()
        val proc = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time", "--pid=$pid"))
        val lines = proc.inputStream.bufferedReader().use { it.readLines() }
        lines.takeLast(MAX_LOG_LINES).joinToString("\n")
    }.getOrElse { "(could not read logs: ${it.message})" }

    /** Mask known secret values, then scrub credential-shaped patterns. */
    fun redact(text: String, maskValues: List<String>): String {
        var out = text
        for (secret in maskValues) {
            if (secret.length >= 4) out = out.replace(secret, REDACTED)
        }
        for (re in PATTERNS) out = re.replace(out, REDACTED)
        return out
    }

    private val PATTERNS = listOf(
        // Authorization: Bearer <token>
        Regex("""(?i)(bearer)\s+[A-Za-z0-9._\-]{8,}"""),
        // key/token/secret/password = value  (json, query, kv)
        Regex("""(?i)(api[_-]?key|token|secret|password|authorization)["'\s:=]+[A-Za-z0-9._\-]{6,}"""),
        // long base64-ish blobs (keys)
        Regex("""[A-Za-z0-9+/_\-]{40,}={0,2}"""),
    )

    /** Write the report to cache and return a shareable content:// uri. */
    fun writeShareFile(context: Context, report: String): android.net.Uri {
        val dir = File(context.cacheDir, "diagnostics").apply { mkdirs() }
        val file = File(dir, "hermes-dispatch-report.txt")
        file.writeText(report)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
