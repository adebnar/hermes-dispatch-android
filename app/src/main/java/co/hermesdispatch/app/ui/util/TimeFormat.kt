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

    /** Coarse grouping label for lists: Today / Yesterday / This week / Earlier. */
    fun bucket(epochMillis: Long): String {
        if (epochMillis <= 0L) return "Earlier"
        val now = System.currentTimeMillis()
        val day = DateUtils.DAY_IN_MILLIS
        val ageDays = (startOfDay(now) - startOfDay(epochMillis)) / day
        return when {
            ageDays <= 0 -> "Today"
            ageDays == 1L -> "Yesterday"
            ageDays < 7 -> "This week"
            else -> "Earlier"
        }
    }

    private fun startOfDay(epochMillis: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = epochMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
