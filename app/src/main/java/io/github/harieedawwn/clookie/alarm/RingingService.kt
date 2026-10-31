package io.github.harieedawwn.clookie.alarm

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.harieedawwn.clookie.data.AlarmRepository
import io.github.harieedawwn.clookie.data.RingStateStore
import io.github.harieedawwn.clookie.model.Alarm
import io.github.harieedawwn.clookie.model.RingEffect
import io.github.harieedawwn.clookie.model.RingEvent
import io.github.harieedawwn.clookie.model.RingOutcome
import io.github.harieedawwn.clookie.model.RingRules
import io.github.harieedawwn.clookie.model.RingState
import io.github.harieedawwn.clookie.model.on
import io.github.harieedawwn.clookie.model.waveform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.ceil

/** In-process view of the ring for the ringing screen; the service is the only writer. */
object RingEngine {
    private val _state = MutableStateFlow<RingState?>(null)
    private val duckUntil = MutableStateFlow(0L)

    fun state(context: Context): StateFlow<RingState?> {
        if (_state.value == null) _state.value = RingStateStore(context).read()
        return _state.asStateFlow()
    }

    internal fun publish(state: RingState) {
        _state.value = state
    }

    /** Mission interaction: duck to 30% until 20 s after the last call. */
    fun interact() {
        duckUntil.value = SystemClock.elapsedRealtime() + DuckMs
    }

    internal fun ducked(): Boolean = SystemClock.elapsedRealtime() < duckUntil.value

    private const val DuckMs = 20_000L
}

/**
 * Foreground service that rings: alarm-usage audio (plays in silent and vibrate mode),
 * volume ramp, vibration, wake lock and the full-screen notification. Every transition goes
 * through the pure ring state machine and is persisted before side effects run.
 */
class RingingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private lateinit var repo: AlarmRepository
    private lateinit var store: RingStateStore
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var focus: AudioFocusRequest? = null
    private var savedAlarmVolume: Int? = null
    private var volumeJob: Job? = null
    private var tickJob: Job? = null
    private var ringing: Alarm? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repo = AlarmRepository.get(this)
        store = RingStateStore(this)
        Notifications.createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val now = System.currentTimeMillis()
        val action = intent?.action ?: ACTION_RESTORE
        val triggerAt = intent?.getLongExtra(AlarmReceiver.EXTRA_TRIGGER_AT, now) ?: now
        // Foreground first, within the start window, with the full-screen intent already attached.
        goForeground(Notifications.ringing(this, ringing, (store.read() as? RingState.Ringing)?.triggerAt ?: triggerAt))
        scope.launch {
            mutex.withLock {
                when (action) {
                    ACTION_FIRE -> {
                        val id = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1) ?: -1
                        val claimed = repo.claimFire(id, triggerAt)
                        if (claimed != null) transition(RingEvent.Fire(id, triggerAt, now))
                    }
                    ACTION_SNOOZE -> {
                        val r = store.read() as? RingState.Ringing
                        val a = r?.let { repo.get(it.alarmId) }
                        if (a != null) transition(RingEvent.Snooze(now, a.snoozeMinutes, a.snoozeCount, a.snoozeLimit))
                    }
                    ACTION_DISMISS -> transition(RingEvent.Dismiss(now))
                    else -> transition(RingEvent.Restore(now))
                }
                if (store.read() == RingState.Idle) shutdown()
            }
        }
        return START_STICKY
    }

    private suspend fun transition(event: RingEvent) {
        val t = store.read().on(event)
        store.write(t.state)
        RingEngine.publish(t.state)
        for (effect in t.effects) apply(effect)
    }

    private suspend fun apply(effect: RingEffect) {
        when (effect) {
            is RingEffect.StartRinging -> {
                if (ringing?.id == effect.alarmId && player != null) return // already ringing (restore while alive)
                val alarm = repo.get(effect.alarmId)
                if (alarm == null) {
                    // Deleted while ringing: nothing to show, stop cleanly.
                    transition(RingEvent.Dismiss(System.currentTimeMillis()))
                    return
                }
                startRinging(alarm)
            }
            RingEffect.StopRinging -> stopRinging()
            is RingEffect.ScheduleSnooze -> repo.snooze(effect.alarmId, effect.at, effect.count)
            is RingEffect.Finish -> {
                val alarm = repo.get(effect.alarmId)
                repo.finish(effect.alarmId, effect.outcome, effect.triggerAt, System.currentTimeMillis())
                if (effect.outcome == RingOutcome.Missed && alarm != null) Notifications.missed(this, alarm, effect.triggerAt)
            }
        }
    }

    private fun startRinging(alarm: Alarm) {
        stopRinging()
        ringing = alarm
        val r = store.read() as? RingState.Ringing
        val n = Notifications.ringing(this, alarm, r?.triggerAt ?: System.currentTimeMillis())
        runCatching { NotificationManagerCompat.from(this).notify(Notifications.ID_RINGING, n) }

        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "clookie:ringing")
            .apply { acquire(RingRules.AutoSilenceMs + 60_000L) }

        val audio = getSystemService(AudioManager::class.java)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        savedAlarmVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        val target = ceil(max * alarm.volume.coerceIn(10, 100) / 100.0).toInt().coerceIn(1, max)
        runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, target, 0) }
        focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(Sounds.alarmAttributes)
            .setOnAudioFocusChangeListener { }
            .build()
            .also { runCatching { audio.requestAudioFocus(it) } }

        player = Sounds.playerWithFallback(this, alarm.sound)?.apply {
            setVolume(0f, 0f)
            setOnErrorListener { mp, _, _ ->
                // Chosen source broke mid-ring: switch to the bundled sound.
                mp.release()
                player = Sounds.playerWithFallback(this@RingingService, SoundRef.Fallback.id)?.apply { start() }
                true
            }
            start()
        }
        startVibration(alarm)

        val started = SystemClock.elapsedRealtime()
        volumeJob = scope.launch {
            while (isActive) {
                val p = if (alarm.rampSeconds <= 0) 1f
                else ((SystemClock.elapsedRealtime() - started) / (alarm.rampSeconds * 1000f)).coerceIn(0f, 1f)
                // Perceptual ramp from quiet to full; ducked to 30% while a mission is in use.
                val v = (0.08f + 0.92f * p * p) * (if (RingEngine.ducked()) 0.3f else 1f)
                runCatching { player?.setVolume(v, v) }
                delay(100)
            }
        }
        tickJob = scope.launch {
            while (isActive) {
                delay(30_000)
                mutex.withLock {
                    transition(RingEvent.Tick(System.currentTimeMillis()))
                    if (store.read() == RingState.Idle) shutdown()
                }
            }
        }
    }

    private fun startVibration(alarm: Alarm) {
        val wave = alarm.vibration.waveform ?: return
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(Vibrator::class.java)
        }
        if (!v.hasVibrator()) return
        val effect = if (v.hasAmplitudeControl()) VibrationEffect.createWaveform(wave.timings, wave.amplitudes, 0)
        else VibrationEffect.createWaveform(wave.timings, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION") v.vibrate(effect, Sounds.alarmAttributes)
        }
        vibrator = v
    }

    private fun stopRinging() {
        volumeJob?.cancel()
        tickJob?.cancel()
        volumeJob = null
        tickJob = null
        player?.runCatching { stop(); release() }
        player = null
        vibrator?.cancel()
        vibrator = null
        val audio = getSystemService(AudioManager::class.java)
        focus?.let { runCatching { audio.abandonAudioFocusRequest(it) } }
        focus = null
        savedAlarmVolume?.let { runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, it, 0) } }
        savedAlarmVolume = null
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        ringing = null
    }

    private fun shutdown() {
        stopRinging()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun goForeground(notification: android.app.Notification) {
        val type = when {
            Build.VERSION.SDK_INT >= 34 -> ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
            Build.VERSION.SDK_INT >= 29 -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            else -> 0
        }
        try {
            ServiceCompat.startForeground(this, Notifications.ID_RINGING, notification, type)
        } catch (e: Exception) {
            // systemExempted refused (no exact-alarm permission): ring as media playback instead.
            if (Build.VERSION.SDK_INT >= 34) {
                runCatching {
                    ServiceCompat.startForeground(this, Notifications.ID_RINGING, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                }
            }
        }
    }

    override fun onDestroy() {
        stopRinging()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_FIRE = "fire"
        const val ACTION_SNOOZE = "snooze"
        const val ACTION_DISMISS = "dismiss"
        const val ACTION_RESTORE = "restore"

        fun fire(context: Context, alarmId: Long, triggerAt: Long) = start(
            context,
            Intent(context, RingingService::class.java).setAction(ACTION_FIRE)
                .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                .putExtra(AlarmReceiver.EXTRA_TRIGGER_AT, triggerAt),
        )

        fun restore(context: Context) = start(context, Intent(context, RingingService::class.java).setAction(ACTION_RESTORE))
        fun snooze(context: Context) = start(context, Intent(context, RingingService::class.java).setAction(ACTION_SNOOZE))
        fun dismiss(context: Context) = start(context, Intent(context, RingingService::class.java).setAction(ACTION_DISMISS))

        /** Notification action into the running service. */
        fun pending(context: Context, action: String): PendingIntent = PendingIntent.getForegroundService(
            context, action.hashCode(), Intent(context, RingingService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun start(context: Context, intent: Intent): Boolean = try {
            ContextCompat.startForegroundService(context, intent)
            true
        } catch (e: Exception) {
            // Background start refused: post the full-screen notification; the ringing screen
            // starts the service itself once it is in the foreground.
            runCatching {
                val at = intent.getLongExtra(AlarmReceiver.EXTRA_TRIGGER_AT, System.currentTimeMillis())
                Notifications.createChannels(context)
                NotificationManagerCompat.from(context).notify(Notifications.ID_RINGING, Notifications.ringing(context, null, at))
                io.github.harieedawwn.clookie.RingingActivity.pendingStart = intent
            }
            false
        }
    }
}
