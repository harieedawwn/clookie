package io.github.harieedawwn.clookie.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.harieedawwn.clookie.MainActivity
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.data.AlarmRepository
import io.github.harieedawwn.clookie.data.SettingsStore
import io.github.harieedawwn.clookie.model.AlarmKind
import io.github.harieedawwn.clookie.model.Bedtime
import io.github.harieedawwn.clookie.model.nextUp
import io.github.harieedawwn.clookie.widget.ClookieWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

/** Everything derived from the alarm list: the bedtime reminder and the home-screen widget. */
object AlarmSideEffects {
    suspend fun refresh(context: Context) {
        runCatching { BedtimeReminder.sync(context) }
        runCatching { ClookieWidget.updateAll(context) }
    }
}

/** "Giờ đi ngủ": one exact reminder at the next alarm minus the sleep goal, re-planned on every change. */
object BedtimeReminder {
    suspend fun sync(context: Context) {
        val store = SettingsStore(context)
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pending(context)
        val at = if (store.bedtimeOn.first()) nextReminder(context, store.sleepGoal.first()) else null
        if (at == null) {
            am.cancel(pi)
            return
        }
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.first, pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.first, pi)
        }
    }

    /** (reminder instant, next alarm "HH:mm") or null. */
    private suspend fun nextReminder(context: Context, goal: Int): Pair<Long, String>? {
        val zone = ZoneId.systemDefault()
        val alarms = AlarmRepository.get(context).alarms.first().filter { it.kind == AlarmKind.Alarm }
        val next = alarms.nextUp(LocalDateTime.now(), zone)?.second?.atZone(zone) ?: return null
        val at = Bedtime.reminderAt(next, goal, java.time.ZonedDateTime.now(zone)) ?: return null
        return at.toInstant().toEpochMilli() to "%02d:%02d".format(next.hour, next.minute)
    }

    internal suspend fun post(context: Context) {
        val store = SettingsStore(context)
        if (!store.bedtimeOn.first()) return
        val zone = ZoneId.systemDefault()
        val alarms = AlarmRepository.get(context).alarms.first().filter { it.kind == AlarmKind.Alarm }
        val next = alarms.nextUp(LocalDateTime.now(), zone)?.second ?: return
        val goal = store.sleepGoal.first()
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, Notifications.CHANNEL_BEDTIME)
            .setSmallIcon(R.drawable.ic_stat_clookie)
            .setContentTitle(context.getString(R.string.bedtime_title))
            .setContentText(context.getString(R.string.bedtime_text, "%02d:%02d".format(next.hour, next.minute), goalText(context, goal)))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(Notifications.ID_BEDTIME, n) }
    }

    fun goalText(context: Context, minutes: Int): String =
        if (minutes % 60 == 0) context.getString(R.string.hours_value, minutes / 60)
        else context.getString(R.string.hours_half_value, minutes / 60)

    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 7, Intent(context, BedtimeReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

class BedtimeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        scope.launch {
            try {
                Notifications.createChannels(app)
                BedtimeReminder.post(app)
                BedtimeReminder.sync(app)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
