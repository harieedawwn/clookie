package io.github.harieedawwn.clookie.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.design.icons.ClookieIcons
import io.github.harieedawwn.clookie.model.Stopwatch
import io.github.harieedawwn.clookie.model.TimerState
import io.github.harieedawwn.clookie.model.countdownParts
import io.github.harieedawwn.clookie.ui.Eyebrow
import io.github.harieedawwn.clookie.ui.components.ButtonKind
import io.github.harieedawwn.clookie.ui.components.CookieButton
import io.github.harieedawwn.clookie.ui.components.TimeDisplay
import io.github.harieedawwn.clookie.ui.components.Txt
import io.github.harieedawwn.clookie.ui.components.styleFor
import io.github.harieedawwn.clookie.ui.screenBackground
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId

/**
 * A clock value that changes on every whole second of [value] (not of the wall clock), so a
 * countdown never shows a stale second. Static when [live] is false (screenshots).
 */
@Composable
private fun rememberSecondTicks(live: Boolean, key: Any, now: () -> Long): State<Long> {
    val state = remember { mutableLongStateOf(now()) }
    LaunchedEffect(live, key) {
        state.longValue = now()
        while (live) {
            delay(1000 - (now() % 1000))
            state.longValue = now()
        }
    }
    return state
}

/** Frame clock for draw-phase animation; only runs while [running] and motion is not reduced. */
@Composable
private fun rememberFrameNow(running: Boolean, now: () -> Long): State<Long> {
    val state = remember { mutableLongStateOf(now()) }
    val reduced = Clookie.motion.reduced
    LaunchedEffect(running, reduced) {
        state.longValue = now()
        if (!running || reduced) return@LaunchedEffect
        while (true) withFrameMillis { state.longValue = now() }
    }
    return state
}

@Composable
private fun ToolTitle(title: String) {
    Txt(title, Clookie.type.displayTitle, Modifier.fillMaxWidth().padding(horizontal = Space.gutter).padding(top = Space.s6))
}

// --- Timer --------------------------------------------------------------------------------

/** Timer: preset minutes, a ring that empties (draw phase), start / pause / resume / reset. */
@Composable
fun TimerScreen(
    state: TimerState,
    onPick: (Long) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    live: Boolean = true,
    wallNow: () -> Long = System::currentTimeMillis,
) {
    val c = Clookie.colors
    val tick by rememberSecondTicks(live && state.running, state) { wallNow() }
    val left = state.left(if (state.running) tick else wallNow())
    val done = state.running && left == 0L
    val frame = rememberFrameNow(live && state.running && !done) { wallNow() }
    Column(modifier.fillMaxSize().screenBackground().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        ToolTitle(stringResource(R.string.nav_timer))
        Spacer(Modifier.weight(0.5f))
        Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val w = 16.dp.toPx()
                val inset = w / 2 + Stroke.regular.toPx()
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                drawCircle(c.slab, size.minDimension / 2 - inset, style = DrawStroke(w))
                drawCircle(c.outline, size.minDimension / 2, style = DrawStroke(Stroke.regular.toPx()))
                val remaining = if (state.running) state.left(maxOf(frame.value, tick)) else left
                val f = (remaining.toFloat() / state.duration.coerceAtLeast(1)).coerceIn(0f, 1f)
                if (f > 0f) {
                    drawArc(c.accent, -90f, 360f * f, false, Offset(inset, inset), arcSize, style = DrawStroke(w, cap = StrokeCap.Round))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val (h, m, s) = countdownParts(left)
                if (h > 0) TimeDisplay(h, m, 72.dp) else TimeDisplay(m, s, 72.dp)
                val status = when {
                    done -> stringResource(R.string.timer_done)
                    state.running -> stringResource(R.string.timer_ends, Instant.ofEpochMilli(state.endAt!!).atZone(ZoneId.systemDefault()).let { "%02d:%02d".format(it.hour, it.minute) })
                    state.paused -> stringResource(R.string.timer_paused)
                    else -> null
                }
                if (status != null) Txt(status, Clookie.type.label, color = if (done) c.accent else c.inkMuted, maxLines = 1)
            }
        }
        Spacer(Modifier.weight(0.5f))
        if (!state.running && !state.paused || done) {
            Column(Modifier.fillMaxWidth().padding(horizontal = Space.gutter)) {
                Eyebrow(stringResource(R.string.timer_presets))
                Spacer(Modifier.height(Space.s2))
                ChoiceChips(
                    TimerState.PresetMinutes.map { it * 60_000L to stringResource(R.string.minutes_short, it) },
                    if (done) -1L else state.duration, onPick,
                )
            }
        }
        Spacer(Modifier.height(Space.s5))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.gutter).padding(bottom = 104.dp),
            horizontalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            when {
                done -> CookieButton(stringResource(R.string.timer_reset), onReset, Modifier.weight(1f), icon = ClookieIcons.Check)
                state.running -> {
                    CookieButton(stringResource(R.string.timer_reset), onReset, Modifier.weight(1f), kind = ButtonKind.Secondary)
                    CookieButton(stringResource(R.string.timer_pause), onPause, Modifier.weight(1f), icon = ClookieIcons.Pause)
                }
                state.paused -> {
                    CookieButton(stringResource(R.string.timer_reset), onReset, Modifier.weight(1f), kind = ButtonKind.Secondary)
                    CookieButton(stringResource(R.string.timer_resume), onStart, Modifier.weight(1f), icon = ClookieIcons.Play)
                }
                else -> CookieButton(stringResource(R.string.timer_start), onStart, Modifier.weight(1f), icon = ClookieIcons.Play)
            }
        }
    }
}

// --- Stopwatch ----------------------------------------------------------------------------

/** "mm:ss.cc", or "h:mm:ss" from one hour. */
fun lapText(ms: Long): String {
    val cs = (ms / 10) % 100
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60) else "%02d:%02d.%02d".format(s / 60, s % 60, cs)
}

/**
 * Stopwatch on elapsedRealtime. Big numerals change once a second; hundredths are drawn in the
 * draw phase from the frame clock (static with reduced motion).
 */
@Composable
fun StopwatchScreen(
    state: Stopwatch,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    live: Boolean = true,
    elapsedNow: () -> Long = SystemClock::elapsedRealtime,
) {
    val c = Clookie.colors
    val tick by rememberSecondTicks(live && state.running, state) { state.elapsed(elapsedNow()) }
    val frame = rememberFrameNow(live && state.running, elapsedNow)
    val shown = if (state.running) tick else state.elapsed(elapsedNow())
    val seconds by remember(shown) { derivedStateOf { shown / 1000 } }
    Column(modifier.fillMaxSize().screenBackground().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        ToolTitle(stringResource(R.string.nav_stopwatch))
        Spacer(Modifier.height(Space.s8))
        Row(verticalAlignment = Alignment.Bottom) {
            if (seconds >= 3600) TimeDisplay((seconds / 3600).toInt(), ((seconds % 3600) / 60).toInt(), 96.dp)
            else TimeDisplay((seconds / 60).toInt(), (seconds % 60).toInt(), 96.dp)
            Hundredths(Modifier.padding(start = Space.s1, bottom = Space.s2), 40.dp) {
                // frame.value is read here, in the draw phase: only this canvas redraws per frame.
                val e = state.elapsed(if (state.running && live) maxOf(frame.value, elapsedNow()) else elapsedNow())
                if (seconds >= 3600) (e / 1000) % 60 else (e / 10) % 100
            }
        }
        Spacer(Modifier.height(Space.s6))
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.gutter), horizontalArrangement = Arrangement.spacedBy(Space.s3)) {
            when {
                state.running -> {
                    CookieButton(stringResource(R.string.sw_lap), onLap, Modifier.weight(1f), kind = ButtonKind.Secondary, icon = ClookieIcons.Mission)
                    CookieButton(stringResource(R.string.timer_pause), onPause, Modifier.weight(1f), icon = ClookieIcons.Pause)
                }
                !state.isReset -> {
                    CookieButton(stringResource(R.string.timer_reset), onReset, Modifier.weight(1f), kind = ButtonKind.Secondary)
                    CookieButton(stringResource(R.string.timer_resume), onStart, Modifier.weight(1f), icon = ClookieIcons.Play)
                }
                else -> CookieButton(stringResource(R.string.timer_start), onStart, Modifier.weight(1f), icon = ClookieIcons.Play)
            }
        }
        Spacer(Modifier.height(Space.s4))
        val splits = state.laps.indices.map { state.split(it) }
        val best = if (splits.size > 1) splits.indices.minBy { splits[it] } else -1
        val worst = if (splits.size > 1) splits.indices.maxBy { splits[it] } else -1
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            itemsIndexed(state.laps.reversed(), key = { i, _ -> state.laps.size - i }) { i, total ->
                val idx = state.laps.size - 1 - i
                LapRow(idx + 1, splits[idx], total, mark = when (idx) { best -> 1; worst -> -1; else -> 0 })
            }
        }
    }
}

@Composable
private fun Hundredths(modifier: Modifier, size: Dp, value: () -> Long) {
    val c = Clookie.colors
    val measurer = rememberTextMeasurer(cacheSize = 100)
    val style = styleFor(with(androidx.compose.ui.platform.LocalDensity.current) { size.toSp() }, Clookie.style.clockFont).merge(color = c.inkMuted)
    val sample = remember(style) { measurer.measure(".00", style) }
    val w = with(androidx.compose.ui.platform.LocalDensity.current) { sample.size.width.toDp() }
    val h = with(androidx.compose.ui.platform.LocalDensity.current) { sample.size.height.toDp() }
    Canvas(modifier.size(w * 1.1f, h)) {
        drawText(measurer, ".%02d".format(value()), style = style)
    }
}

@Composable
private fun LapRow(n: Int, split: Long, total: Long, mark: Int) {
    val c = Clookie.colors
    val shape = Clookie.shapes.medium
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(if (mark == 1) c.slab else c.surface, shape)
            .border(Stroke.thin, c.outline, shape)
            .padding(horizontal = Space.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(if (mark == 1) c.accent else if (mark == -1) c.inkMuted else c.surface, CircleShape))
        Spacer(Modifier.width(Space.s3))
        Txt(stringResource(R.string.sw_lap_n, n), Clookie.type.bodyStrong, Modifier.weight(1f), maxLines = 1)
        Txt(lapText(split), Clookie.type.bodyStrong, color = if (mark == 1) c.accent else c.ink, maxLines = 1)
        Spacer(Modifier.width(Space.s4))
        Txt(lapText(total), Clookie.type.label, color = c.inkMuted, maxLines = 1)
    }
}
