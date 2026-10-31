package io.github.harieedawwn.clookie.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import io.github.harieedawwn.clookie.data.deviceStorage
import io.github.harieedawwn.clookie.model.AwakeEffect
import io.github.harieedawwn.clookie.model.AwakeEvent
import io.github.harieedawwn.clookie.model.AwakeState
import io.github.harieedawwn.clookie.model.on

/**
 * Runs the pure awake-check machine: persists its state in device-protected storage (survives
 * reboot, readable before the first unlock), registers exact alarms for "ask" and "deadline",
 * posts the question and rings the alarm again when it goes unanswered.
 */
object AwakeCheck {
    private val lock = Any()

    fun send(context: Context, event: AwakeEvent) {
        val app = context.applicationContext
        val effects = synchronized(lock) {
            val (state, effects) = read(app).on(event)
            write(app, state)
            effects
        }
        effects.forEach { apply(app, it) }
    }

    private fun apply(context: Context, effect: AwakeEffect) {
        val am = context.getSystemService(AlarmManager::class.java)
        when (effect) {
            is AwakeEffect.ScheduleAt -> {
                val pi = duePending(context)
                try {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, effect.at, pi)
                } catch (e: SecurityException) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, effect.at, pi)
                }
            }
            AwakeEffect.CancelSchedule -> am.cancel(duePending(context))
            is AwakeEffect.Ask -> Notifications.awake(context, effect.until)
            AwakeEffect.ClearAsk -> runCatching { NotificationManagerCompat.from(context).cancel(Notifications.ID_AWAKE) }
            is AwakeEffect.Ring -> RingingService.recheck(context, effect.alarmId)
        }
    }

    private fun duePending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 1, Intent(context, AwakeReceiver::class.java).setAction(AwakeReceiver.ACTION_DUE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** "Còn thức" on the notification (or tapping it). */
    fun confirmPending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 2, Intent(context, AwakeReceiver::class.java).setAction(AwakeReceiver.ACTION_CONFIRM),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun prefs(context: Context) = context.deviceStorage().getSharedPreferences("awake_check", Context.MODE_PRIVATE)

    private fun read(context: Context): AwakeState {
        val p = prefs(context)
        val id = p.getLong("alarm_id", -1)
        val at = p.getLong("at", 0)
        return when (p.getString("phase", null)) {
            "waiting" -> AwakeState.Waiting(id, at)
            "asking" -> AwakeState.Asking(id, at)
            else -> AwakeState.Idle
        }
    }

    private fun write(context: Context, state: AwakeState) {
        val e = prefs(context).edit().clear()
        when (state) {
            AwakeState.Idle -> Unit
            is AwakeState.Waiting -> e.putString("phase", "waiting").putLong("alarm_id", state.alarmId).putLong("at", state.askAt)
            is AwakeState.Asking -> e.putString("phase", "asking").putLong("alarm_id", state.alarmId).putLong("at", state.deadline)
        }
        e.commit()
    }
}

/** Awake-check alarms ("ask now", "deadline") and the "Còn thức" answer. */
class AwakeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DUE -> AwakeCheck.send(context, AwakeEvent.Due(System.currentTimeMillis()))
            ACTION_CONFIRM -> AwakeCheck.send(context, AwakeEvent.Confirmed)
        }
    }

    companion object {
        const val ACTION_DUE = "io.github.harieedawwn.clookie.AWAKE_DUE"
        const val ACTION_CONFIRM = "io.github.harieedawwn.clookie.AWAKE_CONFIRM"
    }
}
