package co.hermesdispatch.app.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import co.hermesdispatch.app.MainActivity
import co.hermesdispatch.app.R

/**
 * Builds the lock-screen "live update" (an ongoing, low-priority progress
 * notification updated as the agent works) and the terminal completion
 * notification. Both deep-link into the relevant task.
 */
object NotificationHelper {
    private const val CHANNEL_PROGRESS = "agent_progress"
    private const val CHANNEL_DONE = "agent_done"

    fun ensureChannels(context: Context) {
        // minSdk is 26 (O), so notification channels always exist.
        val mgr = context.getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_PROGRESS, "Agent progress", NotificationManager.IMPORTANCE_LOW),
        )
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, "Task complete", NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    fun show(context: Context, message: PushMessage) {
        ensureChannels(context)
        val notifId = (message.sessionId ?: message.title).hashCode()
        val builder = if (message.done) {
            NotificationCompat.Builder(context, CHANNEL_DONE)
                .setContentTitle(message.title)
                .setContentText(message.status.ifBlank { "Complete" })
                .setAutoCancel(true)
                .setOngoing(false)
        } else {
            NotificationCompat.Builder(context, CHANNEL_PROGRESS)
                .setContentTitle(message.title)
                .setContentText(message.status)
                .setOngoing(true)
                .setProgress(0, 0, true)
        }
        builder
            .setSmallIcon(R.drawable.ic_stat_dispatch)
            .setOnlyAlertOnce(true)
            .setContentIntent(deepLink(context, message.sessionId, notifId))

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            runCatching { NotificationManagerCompat.from(context).notify(notifId, builder.build()) }
        }
    }

    private fun deepLink(context: Context, sessionId: String?, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            sessionId?.let { putExtra(MainActivity.EXTRA_SESSION_ID, it) }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
