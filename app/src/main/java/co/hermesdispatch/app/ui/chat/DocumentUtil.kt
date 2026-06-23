package co.hermesdispatch.app.ui.chat

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64

/** Reads a user-picked document into a base64 data URL for upload to the bridge. */
object DocumentUtil {
    /** Cap to keep the JSON upload reasonable; larger files should use a link. */
    private const val MAX_BYTES = 10 * 1024 * 1024

    data class Picked(val name: String, val dataUrl: String)

    fun read(context: Context, uri: Uri): Picked? {
        val resolver = context.contentResolver
        val name = displayName(context, uri) ?: "document"
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        if (bytes.isEmpty() || bytes.size > MAX_BYTES) return null
        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return Picked(name, "data:$mime;base64,$b64")
    }

    private fun displayName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
}
