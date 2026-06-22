package co.hermesdispatch.app.ui.util

import android.text.format.DateUtils

/** Human-friendly relative timestamps, e.g. "2 hours ago", "Yesterday". */
object TimeFormat {
    fun relative(epochMillis: Long): String {
        if (epochMillis <= 0L) return ""
        return DateUtils.getRelativeTimeSpanString(
            epochMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    }
}
