package io.github.harieedawwn.clookie.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.harieedawwn.clookie.design.ClockFont
import io.github.harieedawwn.clookie.design.ClockStyle
import io.github.harieedawwn.clookie.design.ClookieTheme
import io.github.harieedawwn.clookie.design.CornerStyle
import io.github.harieedawwn.clookie.design.CustomPreset
import io.github.harieedawwn.clookie.design.Look
import io.github.harieedawwn.clookie.design.Preset
import io.github.harieedawwn.clookie.design.Presets
import io.github.harieedawwn.clookie.model.RingOutcome
import io.github.harieedawwn.clookie.model.SampleAlarms
import io.github.harieedawwn.clookie.model.Stopwatch
import io.github.harieedawwn.clookie.model.TimerState
import io.github.harieedawwn.clookie.model.WakeEntry
import io.github.harieedawwn.clookie.ui.LocalNow
import io.github.harieedawwn.clookie.ui.components.NavDest
import io.github.harieedawwn.clookie.ui.screens.HomeChrome
import io.github.harieedawwn.clookie.ui.screens.MainScaffold
import io.github.harieedawwn.clookie.ui.screens.StopwatchScreen
import io.github.harieedawwn.clookie.ui.screens.StudioDraft
import io.github.harieedawwn.clookie.ui.screens.StudioScreen
import io.github.harieedawwn.clookie.ui.screens.TimerScreen
import io.github.harieedawwn.clookie.ui.screens.WakeLogScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime
import java.time.ZoneId

/** Stage 5 review screenshots in a light and a dark preset -> design/screens/s5_*. Clock: Mon 2026-11-02 23:18:07. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Stage5Shots {
    @get:org.junit.Rule val fontScale = FontScaleRule()

    private val now = mutableStateOf(LocalDateTime.of(2026, 11, 2, 23, 18, 7))
    private val zone = ZoneId.systemDefault()
    private val wall = now.value.atZone(zone).toInstant().toEpochMilli()
    private val presets = listOf(Presets.BoSua, Presets.Dem)

    private fun shot(name: String, preset: Preset, look: Look? = null, content: @Composable () -> Unit) {
        captureRoboImage("../design/screens/$name.png") {
            ClookieTheme(preset, reducedMotion = true, look = look) {
                CompositionLocalProvider(LocalNow provides now) { Box(Modifier.fillMaxSize()) { content() } }
            }
        }
    }

    private val custom = listOf(CustomPreset("c1", "Của tôi 1", Look("matcha", Color(0xFFE85D75), ClockFont.Ui, CornerStyle.Crisp)))

    @Composable
    private fun Studio(p: Preset, draft: StudioDraft? = null) = StudioScreen(
        current = Look(p.id), currentStyle = ClockStyle.Digital, customPresets = custom, reducedMotion = false,
        alarms = SampleAlarms.list, onApply = { _, _ -> }, onSave = { _, _ -> }, onDeleteCustom = {}, onReducedMotion = {},
        onBack = {}, initial = draft,
    )

    @Test
    @Config(qualifiers = "vi-w412dp-h915dp-xhdpi")
    fun studio() {
        for (p in presets) shot("s5_studio_${p.id}", p) { Studio(p, StudioDraft(Look(p.id), ClockStyle.Cookie)) }
        // Light yellow on cream fails 3:1: the studio shows picked -> adjusted.
        shot("s5_studio_adjusted_bo_sua", Presets.BoSua) {
            Studio(Presets.BoSua, StudioDraft(Look("bo_sua", accent = Color(0xFFF4D35E), clockFont = ClockFont.Ui), ClockStyle.Words))
        }
    }

    @Test
    @Config(qualifiers = "vi-w412dp-h915dp-xhdpi")
    fun clockStyles() {
        for (p in presets) for (s in ClockStyle.entries) {
            shot("s5_clock_${s.name.lowercase()}_${p.id}", p) { HomeChrome(SampleAlarms.list, reliabilityIssue = false, clockStyle = s) }
        }
        // A custom look applied to the whole Home.
        val look = Look("matcha", Color(0xFFE85D75), ClockFont.Ui, CornerStyle.Crisp)
        shot("s5_clock_custom_matcha", Presets.Matcha, look) { HomeChrome(SampleAlarms.list, reliabilityIssue = false, clockStyle = ClockStyle.Cookie) }
    }

    @Test
    @Config(qualifiers = "vi-w412dp-h915dp-xhdpi")
    fun timerAndStopwatch() {
        val running = TimerState(duration = 10 * 60_000L, endAt = wall + 7 * 60_000L + 42_000, alarmId = 1)
        val sw = Stopwatch(accumulated = 0, startedAt = 0, laps = listOf(61_420, 118_050, 182_730, 239_900))
        for (p in presets) {
            shot("s5_timer_${p.id}", p) {
                MainScaffold(NavDest.Timer, {}) { TimerScreen(running, {}, {}, {}, {}, live = false, wallNow = { wall }) }
            }
            shot("s5_timer_idle_${p.id}", p) {
                MainScaffold(NavDest.Timer, {}) { TimerScreen(TimerState(duration = 25 * 60_000L), {}, {}, {}, {}, live = false, wallNow = { wall }) }
            }
            shot("s5_stopwatch_${p.id}", p) {
                MainScaffold(NavDest.Stopwatch, {}) { StopwatchScreen(sw, {}, {}, {}, {}, live = false, elapsedNow = { 263_470 }) }
            }
        }
    }

    @Test
    @Config(qualifiers = "vi-w412dp-h915dp-xhdpi")
    fun wakeLog() {
        val day0 = now.value.toLocalDate()
        // 14 days: mostly on time, a few late or snoozed, one missed, two days without alarms.
        val pattern = listOf(1L to 0, 3L to 0, -1L to 0, 2L to 0, 9L to 2, 1L to 0, -2L to 0, -1L to 0, 4L to 1, 1L to 0, 2L to 0, 1L to 0, 0L to 0, 1L to 0)
        val entries = pattern.mapIndexedNotNull { i, (min, snoozes) ->
            if (min == -1L) return@mapIndexedNotNull null
            val at = day0.minusDays((13 - i).toLong()).atTime(6, 30).atZone(zone).toInstant().toEpochMilli()
            val outcome = if (min == -2L) RingOutcome.Missed else RingOutcome.Dismissed
            val stop = if (min == -2L) 30 * 60_000L else min * 60_000L + 25_000
            WakeEntry(if (i % 3 == 0) "Đi làm" else "", at, at, at + stop, outcome, snoozes)
        }.reversed()
        for (p in presets) shot("s5_wakelog_${p.id}", p) { WakeLogScreen(entries, day0, onBack = {}, zone = zone) }
    }
}
