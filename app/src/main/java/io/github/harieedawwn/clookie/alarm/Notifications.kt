package io.github.harieedawwn.clookie.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.harieedawwn.clookie.MainActivity
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.RingingActivity
import io.github.harieedawwn.clookie.mission.MissionRegistry
import io.github.harieedawwn.clookie.model.Alarm
import java.time.Instant
import java.time.ZoneId

object Notifications {
    const val CHANNEL_RINGING = "ringing"
    const val CHANNEL_MISSED = "missed"
    const val CHANNEL_UPDATES = "updates"
    const val ID_RINGING = 1
    private const val ID_MISSED_BASE = 1000

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                // Sound and vibration come from the service, so the channel itself is silent.
                NotificationChannel(CHANNEL_RINGING, context.getString(R.string.channel_ringing), NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(null, null)
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setShowBadge(false)
                },
                NotificationChannel(CHANNEL_MISSED, context.getString(R.string.channel_missed), NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_UPDATES, context.getString(R.string.channel_updates), NotificationManager.IMPORTANCE_LOW),
            ),
        )
    }

    /** Ringing notification with the full-screen intent that opens [RingingActivity] over the lock screen. */
    fun ringing(context: Context, alarm: Alarm?, triggerAt: Long): Notification {
        val time = Instant.ofEpochMilli(triggerAt).atZone(ZoneId.systemDefault())
        val title = "%02d:%02d".format(time.hour, time.minute)
        val text = alarm?.label?.takeIf { it.isNotBlank() } ?: context.getString(R.string.nav_alarms)
        val screen = PendingIntent.getActivity(
            context, 0, RingingActivity.intent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val b = NotificationCompat.Builder(context, CHANNEL_RINGING)
            .setSmallIcon(R.drawable.ic_stat_clookie)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .setContentIntent(screen)
            .setFullScreenIntent(screen, true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (alarm != null) {
            if (alarm.snoozeLimit > alarm.snoozeCount && !alarm.snoozeAfterMission) {
                b.addAction(0, context.getString(R.string.snooze_action, alarm.snoozeMinutes), RingingService.pending(context, RingingService.ACTION_SNOOZE))
            }
            if (MissionRegistry.quickDismiss(alarm.missions)) {
                b.addAction(0, context.getString(R.string.action_stop), RingingService.pending(context, RingingService.ACTION_DISMISS))
            }
        }
        return b.build()
    }

    fun missed(context: Context, alarm: Alarm, triggerAt: Long) {
        val time = Instant.ofEpochMilli(triggerAt).atZone(ZoneId.systemDefault())
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_MISSED)
            .setSmallIcon(R.drawable.ic_stat_clookie)
            .setContentTitle(context.getString(R.string.missed_title, "%02d:%02d".format(time.hour, time.minute)))
            .setContentText(alarm.label.ifBlank { context.getString(R.string.nav_alarms) })
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(ID_MISSED_BASE + alarm.id.toInt(), n) }
    }
}
