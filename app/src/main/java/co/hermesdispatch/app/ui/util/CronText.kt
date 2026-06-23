package co.hermesdispatch.app.ui.util

/**
 * Best-effort human-readable rendering of common cron expressions for the
 * Scheduled list (e.g. "Every weekday at 9:00 AM", "Every 10 minutes"). Falls
 * back to the raw expression for anything it doesn't recognize, so an unusual
 * schedule is never hidden.
 */
object CronText {
    private val DAYS = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    fun humanize(expr: String): String {
        val cron = expr.trim()
        if (cron.isEmpty()) return ""

        // Shorthand the agent/classifier may emit.
        when (cron.lowercase()) {
            "@hourly" -> return "Every hour"
            "@daily", "@midnight" -> return "Every day at 12:00 AM"
            "@weekly" -> return "Every week"
            "@monthly" -> return "Every month"
            "@yearly", "@annually" -> return "Every year"
        }
        Regex("""^every\s+(\d+)\s*(m|min|minutes?|h|hours?)$""", RegexOption.IGNORE_CASE)
            .find(cron)?.let { m ->
                val n = m.groupValues[1]
                val unit = if (m.groupValues[2].startsWith("h", true)) "hour" else "minute"
                return "Every $n $unit${if (n == "1") "" else "s"}"
            }

        val f = cron.split(Regex("\\s+"))
        if (f.size != 5) return cron
        val (min, hour, dom, mon, dow) = f

        // Every N minutes / every minute.
        if (hour == "*" && dom == "*" && mon == "*" && dow == "*") {
            if (min == "*") return "Every minute"
            Regex("""^\*/(\d+)$""").find(min)?.let { return "Every ${it.groupValues[1]} minutes" }
            if (min == "0") return "Every hour, on the hour"
        }

        val time = clockTime(min, hour) ?: return cron
        return when {
            dom == "*" && mon == "*" && dow == "*" -> "Every day at $time"
            dom == "*" && mon == "*" && dow == "1-5" -> "Every weekday at $time"
            dom == "*" && mon == "*" && dow == "0,6" -> "Every weekend at $time"
            dom == "*" && mon == "*" && dow.toIntOrNull() in 0..6 ->
                "Every ${DAYS[dow.toInt() % 7]} at $time"
            dow == "*" && mon == "*" && dom.toIntOrNull() != null ->
                "Monthly on the ${ordinal(dom.toInt())} at $time"
            else -> cron
        }
    }

    /** "9:00 AM" from cron minute+hour fields, or null if not a fixed time. */
    private fun clockTime(min: String, hour: String): String? {
        val h = hour.toIntOrNull() ?: return null
        val m = min.toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        val period = if (h < 12) "AM" else "PM"
        val h12 = when (h % 12) { 0 -> 12; else -> h % 12 }
        return "%d:%02d %s".format(h12, m, period)
    }

    private fun ordinal(n: Int): String {
        val suffix = if (n in 11..13) "th" else when (n % 10) {
            1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th"
        }
        return "$n$suffix"
    }
}
