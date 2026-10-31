package io.github.harieedawwn.clookie

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import io.github.harieedawwn.clookie.alarm.AlarmEngine
import io.github.harieedawwn.clookie.alarm.Notifications
import io.github.harieedawwn.clookie.alarm.RingEngine
import io.github.harieedawwn.clookie.data.AlarmRepository
import io.github.harieedawwn.clookie.data.SettingsStore
import io.github.harieedawwn.clookie.model.RingState
import io.github.harieedawwn.clookie.ui.ClookieApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** AppCompatActivity only for per-app language switching on Android 8-12. */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Notifications.createChannels(this)
        val store = SettingsStore(applicationContext)
        val repo = AlarmRepository.get(applicationContext)
        // Tiny DataStore file: read once so the first frame has the right preset and screen.
        val (preset, onboarded) = runBlocking { store.presetNow() to store.onboarded.first() }
        // Opening the app re-registers every alarm (cheap; heals any lost AlarmManager entry).
        lifecycleScope.launch { AlarmEngine.reschedule(applicationContext) }
        setContent { ClookieApp(store, repo, window, preset, onboarded) }
    }

    override fun onResume() {
        super.onResume()
        // While an alarm rings, the app opens on the ringing screen.
        if (RingEngine.state(this).value is RingState.Ringing) startActivity(RingingActivity.intent(this))
    }
}
