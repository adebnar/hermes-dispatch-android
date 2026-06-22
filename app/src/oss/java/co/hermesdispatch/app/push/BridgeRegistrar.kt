package co.hermesdispatch.app.push

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal, dependency-free POST of this device's push endpoint to the bridge's
 * `/v1/push/register`. Runs from the UnifiedPush [BroadcastReceiver], which has
 * no Hilt/Ktor available, so it uses [HttpURLConnection] on a background thread.
 */
object BridgeRegistrar {

    fun register(bridgeUrl: String?, token: String?, endpoint: String) =
        post(bridgeUrl, token, endpoint, "register")

    fun unregister(bridgeUrl: String?, token: String?, endpoint: String) =
        post(bridgeUrl, token, endpoint, "unregister")

    /** Register the E2EE push key so the bridge encrypts payloads for this device. */
    fun registerKey(bridgeUrl: String?, token: String?, key: String) {
        if (bridgeUrl.isNullOrBlank() || key.isBlank()) return
        postBody(URL("${bridgeUrl.trimEnd('/')}/v1/push/key"), token, """{"key":${jsonString(key)}}""")
    }

    private fun post(bridgeUrl: String?, token: String?, endpoint: String, action: String) {
        if (bridgeUrl.isNullOrBlank() || endpoint.isBlank()) return
        postBody(
            URL("${bridgeUrl.trimEnd('/')}/v1/push/$action"),
            token,
            """{"endpoint":${jsonString(endpoint)}}""",
        )
    }

    private fun postBody(url: URL, token: String?, body: String) {
        var conn: HttpURLConnection? = null
        try {
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 10_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            conn.responseCode // force the request to be sent
        } catch (_: IOException) {
            // Best-effort; the next endpoint event or re-pair will retry.
        } finally {
            conn?.disconnect()
        }
    }

    private fun jsonString(value: String): String {
        val sb = StringBuilder("\"")
        for (c in value) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                else -> sb.append(c)
            }
        }
        return sb.append('"').toString()
    }
}
