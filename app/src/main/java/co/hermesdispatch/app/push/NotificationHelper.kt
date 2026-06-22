package co.hermesdispatch.app.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import co.hermesdispatch.app.MainActivity
import co.hermesdispatch.app.R
import co.hermesdispatch.app.data.prefs.SecureSettings

/**
 * Builds the lock-screen "live update" (an ongoing, low-priority progress
 * notification updated as the agent works), the terminal completion
 * notification, and Inbox alerts (a dedicated high-importance channel whose
 * sound the user can set). Inbox/failure alerts are distinguished from task
 * progress by carrying no sessionId.
 */
object NotificationHelper {
    private const val CHANNEL_PROGRESS = "agent_progress"
    private const val CHANNEL_DONE = "agent_done"
    private const val CHANNEL_INBOX_PREFIX = "inbox_alerts_v"

    /** Channel id for Inbox alerts — versioned so a sound change mints a fresh one. */
    private fun inboxChannelId(settings: SecureSettings) =
        CHANNEL_INBOX_PREFIX + settings.alertChannelVersion()

    fun ensureChannels(context: Context) {
        // minSdk is 26 (O), so notification channels always exist.
        val mgr = context.getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_PROGRESS, "Agent progress", NotificationManager.IMPORTANCE_LOW),
        )
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, "Task complete", NotificationManager.IMPORTANCE_DEFAULT),
        )
        ensureInboxChannel(context, SecureSettings(context))
    }

    private fun ensureInboxChannel(context: Context, settings: SecureSettings) {
        val mgr = context.getSystemService(NotificationManager::class.java)
        val id = inboxChannelId(settings)
        if (mgr.getNotificationChannel(id) != null) return
        val channel = NotificationChannel(id, "Inbox alerts", NotificationManager.IMPORTANCE_HIGH)
        when (val pref = settings.alertSoundUri()) {
            null -> channel.setSound(
                Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            "" -> channel.setSound(null, null) // Silent
            else -> channel.setSound(
                Uri.parse(pref),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        mgr.createNotificationChannel(channel)
    }

    /**
     * Change the Inbox alert sound. Android ignores sound changes to an existing
     * channel, so we bump the version (new channel id) and drop the old one.
     * [uri] null = system default, "silent" intent = pass null after storing "".
     */
    fun applyAlertSound(context: Context, uri: Uri?, silent: Boolean) {
        val settings = SecureSettings(context)
        val mgr = context.getSystemService(NotificationManager::class.java)
        runCatching { mgr.deleteNotificationChannel(inboxChannelId(settings)) }
        settings.setAlertSoundUri(if (silent) "" else uri?.toString())
        settings.setAlertChannelVersion(settings.alertChannelVersion() + 1)
        ensureInboxChannel(context, settings)
    }

    fun show(context: Context, message: PushMessage) {
        ensureChannels(context)
        val settings = SecureSettings(context)
        // Inbox/failure alerts carry no sessionId; task progress/completion do.
        val isInboxAlert = message.done && message.sessionId == null
        val notifId = (message.sessionId ?: message.title).hashCode()
        val builder = if (message.done) {
            val channel = if (isInboxAlert) inboxChannelId(settings) else CHANNEL_DONE
            NotificationCompat.Builder(context, channel)
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
