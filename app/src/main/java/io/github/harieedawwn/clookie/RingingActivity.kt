package io.github.harieedawwn.clookie

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import io.github.harieedawwn.clookie.alarm.RingEngine
import io.github.harieedawwn.clookie.alarm.RingingService
import io.github.harieedawwn.clookie.data.AlarmRepository
import io.github.harieedawwn.clookie.data.RingStateStore
import io.github.harieedawwn.clookie.data.SettingsStore
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.ClookieTheme
import io.github.harieedawwn.clookie.design.Presets
import io.github.harieedawwn.clookie.model.Alarm
import io.github.harieedawwn.clookie.model.RingState
import io.github.harieedawwn.clookie.ui.LocalNow
import io.github.harieedawwn.clookie.ui.rememberTicker
import io.github.harieedawwn.clookie.ui.screens.RingingScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

/**
 * Full-screen ringing screen, shown over the lock screen by the ringing notification's
 * full-screen intent. Closes itself when the ring ends; Back does nothing while ringing; the
 * volume keys cannot lower a volume-locked alarm.
 */
class RingingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        showOverLockScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Fallback path: the service could not start from the background, start it now we are visible.
        val waitForRing = pendingStart != null
        pendingStart?.let { runCatching { ContextCompat.startForegroundService(this, it) } }
        pendingStart = null

        val presetId = runBlocking { SettingsStore(this@RingingActivity).presetNow() }
        val repo = AlarmRepository.get(this)
        val ringState = RingEngine.state(this)
        setContent {
            val state by ringState.collectAsState()
            val ring = state as? RingState.Ringing
            var seenRinging by remember { mutableStateOf(false) }
            LaunchedEffect(ring) {
                if (ring != null) {
                    seenRinging = true
                } else {
                    // Opened before the service published the ring (cold start after boot can be slow);
                    // a stale notification after the ring ended closes at once.
                    if (!seenRinging && (waitForRing || RingStateStore(this@RingingActivity).read() is RingState.Ringing)) delay(15_000)
                    finish()
                }
            }
            val alarm by produceState(initialValue = null as Alarm?, ring?.alarmId) {
                value = ring?.let { repo.get(it.alarmId) }
            }
            BackHandler { }
            ClookieTheme(Presets.byId(presetId)) {
                CompositionLocalProvider(LocalNow provides rememberTicker()) {
                    val a = alarm
                    SideEffect { lockVolume = a?.volumeLock == true }
                    if (a == null) {
                        Box(Modifier.fillMaxSize().background(Clookie.colors.ringing))
                    } else {
                        RingingScreen(
                            alarm = a,
                            date = LocalDate.now(),
                            preview = false,
                            onDismissed = { RingingService.dismiss(this@RingingActivity) },
                            onSnooze = { RingingService.snooze(this@RingingActivity) },
                            onInteract = RingEngine::interact,
                            chainKey = ring?.let { "${it.alarmId}:${it.triggerAt}" },
                        )
                    }
                }
            }
        }
    }

    // The service brings this screen back while the alarm rings and it is not visible.
    override fun onStart() {
        super.onStart()
        RingEngine.screenVisible = true
    }

    override fun onStop() {
        RingEngine.screenVisible = false
        super.onStop()
    }

    /** Volume lock: the volume keys cannot lower or mute the ringing alarm. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (lockVolume && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE)) return true
        return super.onKeyDown(keyCode, event)
    }

    @Volatile private var lockVolume = false

    @Suppress("DEPRECATION")
    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        /** Service start that was refused in the background, retried once this screen is visible. */
        @Volatile var pendingStart: Intent? = null

        fun intent(context: Context): Intent = Intent(context, RingingActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }
}
