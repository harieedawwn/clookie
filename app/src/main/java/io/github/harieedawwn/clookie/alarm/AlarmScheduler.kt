package io.github.harieedawwn.clookie.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import io.github.harieedawwn.clookie.MainActivity

/**
 * AlarmManager wrapper: one setAlarmClock per alarm (shown as the system's next alarm,
 * exempt from Doze and from background FGS-start limits). Falls back to an inexact
 * while-idle alarm only if exact alarms are denied, which Độ tin cậy reports.
 */
class AlarmScheduler(context: Context) {
    private val context = context.applicationContext
    private val am = context.getSystemService(AlarmManager::class.java)

    fun schedule(alarmId: Long, triggerAt: Long) {
        val fire = firePendingIntent(alarmId, triggerAt)
        try {
            if (canScheduleExact()) {
                am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showPendingIntent()), fire)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, fire)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, fire)
        }
    }

    fun cancel(alarmId: Long) {
        am.cancel(firePendingIntent(alarmId, 0))
    }

    fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()

    private fun firePendingIntent(alarmId: Long, triggerAt: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_FIRE)
            .setData(Uri.parse("clookie://alarm/$alarmId"))
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            .putExtra(AlarmReceiver.EXTRA_TRIGGER_AT, triggerAt)
        return PendingIntent.getBroadcast(
            context, alarmId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Tapping the system's "next alarm" opens Clookie. */
    private fun showPendingIntent(): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
