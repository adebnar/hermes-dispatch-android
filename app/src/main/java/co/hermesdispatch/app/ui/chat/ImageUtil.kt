package co.hermesdispatch.app.ui.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** Helpers to turn a camera/gallery image into a compact base64 data URL. */
object ImageUtil {
    private const val MAX_DIM = 1280
    private const val JPEG_QUALITY = 80

    fun bitmapToDataUrl(bitmap: Bitmap): String {
        val scaled = downscale(bitmap)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        return "data:image/jpeg;base64,$b64"
    }

    fun uriToDataUrl(context: Context, uri: Uri): String? = runCatching {
        val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        bmp?.let { bitmapToDataUrl(it) }
    }.getOrNull()

    private fun downscale(bitmap: Bitmap): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= MAX_DIM) return bitmap
        val ratio = MAX_DIM.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt(),
            (bitmap.height * ratio).toInt(),
            true,
        )
    }
}
