package io.github.harieedawwn.clookie.data

import android.content.Context
import io.github.harieedawwn.clookie.model.RingState

/**
 * Persisted [RingState]: synchronous commit to device-protected storage, so the ring
 * survives process death and reboot (read before the first unlock).
 */
class RingStateStore(context: Context) {
    private val prefs = context.deviceStorage().getSharedPreferences("ring_state", Context.MODE_PRIVATE)

    fun read(): RingState {
        val id = prefs.getLong("alarm_id", -1)
        if (id < 0) return RingState.Idle
        return RingState.Ringing(id, prefs.getLong("trigger_at", 0), prefs.getLong("started_at", 0))
    }

    fun write(state: RingState) {
        val e = prefs.edit().clear()
        if (state is RingState.Ringing) {
            e.putLong("alarm_id", state.alarmId).putLong("trigger_at", state.triggerAt).putLong("started_at", state.startedAt)
        }
        e.commit()
    }
}
