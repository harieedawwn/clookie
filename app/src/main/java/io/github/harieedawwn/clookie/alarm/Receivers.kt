package io.github.harieedawwn.clookie.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.harieedawwn.clookie.data.AlarmRepository
import io.github.harieedawwn.clookie.data.RingStateStore
import io.github.harieedawwn.clookie.model.AwakeEvent
import io.github.harieedawwn.clookie.model.RingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** setAlarmClock fired: hand over to the ringing service at once (exact alarms may start an FGS). */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val id = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        val at = intent.getLongExtra(EXTRA_TRIGGER_AT, 0)
        if (id >= 0) RingingService.fire(context, id, at)
    }

    companion object {
        const val ACTION_FIRE = "io.github.harieedawwn.clookie.FIRE"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_TRIGGER_AT = "trigger_at"
    }
}

/**
 * Boot (also before the first unlock), clock or zone change, app update, exact-alarm grant:
 * re-register every alarm, ring slightly late ones, report missed ones, resume a ring that
 * was in progress and a pending awake check.
 */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        scope.launch {
            try {
                AlarmEngine.reschedule(app)
                if (RingStateStore(app).read() is RingState.Ringing) RingingService.restore(app)
                // Re-registers a pending awake check (alarms are cleared on reboot).
                AwakeCheck.send(app, AwakeEvent.Due(System.currentTimeMillis()))
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

/** Entry points shared by receivers and the app. */
object AlarmEngine {
    suspend fun reschedule(context: Context) {
        Notifications.createChannels(context)
        val result = AlarmRepository.get(context).rescheduleAll()
        result.missed.forEach { (alarm, at) -> Notifications.missed(context, alarm, at) }
        result.ringNow?.let { (alarm, at) -> RingingService.fire(context, alarm.id, at) }
    }
}
