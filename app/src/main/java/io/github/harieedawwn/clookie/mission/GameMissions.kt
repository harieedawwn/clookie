package io.github.harieedawwn.clookie.mission

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.design.rememberHaptics
import io.github.harieedawwn.clookie.model.CrumbsGame
import io.github.harieedawwn.clookie.model.MemorySequence
import io.github.harieedawwn.clookie.model.MissionKeys
import io.github.harieedawwn.clookie.model.MissionTuning
import io.github.harieedawwn.clookie.model.MissionType
import io.github.harieedawwn.clookie.ui.components.Txt
import kotlin.random.Random

private enum class MemoryPhase { Watch, Repeat, Wrong }

/**
 * Memory: watch tiles light up one by one, then tap them in the same order. A wrong tap shows a
 * new sequence, so random tapping never gets through. Drawn on a Canvas from frame time.
 */
object MemoryMission : WakeMission {
    private val t = MissionTuning.Memory
    override val type = MissionType.Memory
    override val settings = listOf(
        MissionSetting(MissionKeys.Level, R.string.setting_level, listOf(1, 2, 3), t.DefaultLevel, LevelLabels),
        MissionSetting(MissionKeys.Rounds, R.string.setting_rounds, t.rounds, t.DefaultRounds),
    )

    @Composable
    override fun Content(session: MissionSession, modifier: Modifier) {
        val c = Clookie.colors
        val haptics = rememberHaptics()
        val demo = LocalMissionDemo.current
        val density = LocalDensity.current
        val i = (session.spec.param(MissionKeys.Level, t.DefaultLevel) - 1).coerceIn(0, 2)
        val rounds = session.spec.param(MissionKeys.Rounds, t.DefaultRounds).coerceIn(1, 5)
        val n = t.grid[i]
        val length = t.sequence[i]
        val step = t.litMs[i] + t.gapMs[i]
        val watchMs = t.StartDelayMs + length * step
        val seed = rememberSaveable { Random.nextLong() }
        var round by rememberSaveable { mutableIntStateOf(0) }
        var attempt by rememberSaveable { mutableIntStateOf(0) }
        var phase by remember { mutableStateOf(if (demo) MemoryPhase.Repeat else MemoryPhase.Watch) }
        var entered by remember { mutableIntStateOf(if (demo) 2 else 0) }
        val sequence = remember(round, attempt) { MemorySequence.generate(n * n, length, Random(seed + round * 7919L + attempt)) }
        // Game clock (ms since the phase began), read only while drawing.
        val clock = remember { mutableLongStateOf(if (demo) t.TapFlashMs / 2 else 0L) }
        var tapCell by remember { mutableIntStateOf(if (demo) sequence[1] else -1) }
        var tapAt by remember { mutableLongStateOf(0L) }
        LaunchedEffect(phase, round, attempt) {
            if (demo) return@LaunchedEffect
            val start = withFrameMillis { it }
            while (true) {
                val now = withFrameMillis { it } - start
                clock.longValue = now
                if (phase == MemoryPhase.Watch && now >= watchMs) {
                    phase = MemoryPhase.Repeat
                    break
                }
                if (phase == MemoryPhase.Wrong && now >= t.WrongPauseMs) {
                    entered = 0
                    attempt++
                    phase = MemoryPhase.Watch
                    break
                }
            }
        }
        fun tap(cell: Int) {
            session.interact()
            if (phase != MemoryPhase.Repeat) return
            tapCell = cell
            tapAt = clock.longValue
            if (sequence[entered] != cell) {
                haptics.reject()
                phase = MemoryPhase.Wrong
                return
            }
            haptics.tick()
            entered++
            if (entered < length) return
            haptics.confirm()
            entered = 0
            if (round + 1 >= rounds) session.complete() else {
                round++
                phase = MemoryPhase.Watch
            }
        }
        val gapPx = with(density) { t.TileGapDp.dp.toPx() }
        val stroke = with(density) { Stroke.regular.toPx() }
        val corner = with(density) { Clookie.shapes.smallRadius.toPx() }
        BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth, maxHeight - 40.dp, t.MaxSideDp.dp).coerceAtLeast(180.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val label = when (phase) {
                    MemoryPhase.Watch -> stringResource(R.string.memory_watch)
                    MemoryPhase.Repeat -> stringResource(R.string.memory_repeat, entered, length)
                    MemoryPhase.Wrong -> stringResource(R.string.memory_wrong)
                }
                val roundText = if (rounds > 1) stringResource(R.string.round_progress, round + 1, rounds) + " · " else ""
                Txt((roundText + label).uppercase(), Clookie.type.eyebrow, color = c.onRinging, maxLines = 1)
                Spacer(Modifier.height(Space.s3))
                Canvas(
                    Modifier
                        .size(side)
                        .pointerInput(n, phase, sequence) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val cell = cellAt(down.position, size.width.toFloat(), n, gapPx)
                                if (cell >= 0) tap(cell)
                            }
                        },
                ) {
                    val now = clock.longValue
                    val lit = when (phase) {
                        MemoryPhase.Watch -> {
                            val p = now - t.StartDelayMs
                            if (p < 0) -1 else (p / step).toInt().takeIf { it < length && p % step < t.litMs[i] }?.let { sequence[it] } ?: -1
                        }
                        else -> if (now - tapAt < t.TapFlashMs || phase == MemoryPhase.Wrong) tapCell else -1
                    }
                    drawTiles(n, gapPx, stroke, corner, lit, cross = phase == MemoryPhase.Wrong, c.onRinging, c.ringing)
                }
            }
        }
    }
}

private fun cellAt(p: Offset, side: Float, n: Int, gap: Float): Int {
    val tile = (side - gap * (n - 1)) / n
    val col = (p.x / (tile + gap)).toInt()
    val row = (p.y / (tile + gap)).toInt()
    if (col !in 0 until n || row !in 0 until n) return -1
    // Taps in the gaps between tiles count for the nearest tile: big, forgiving targets.
    return row * n + col
}

/** Outlined tiles; the [lit] one is filled with a bite (or crossed out when [cross]). */
private fun DrawScope.drawTiles(n: Int, gap: Float, stroke: Float, corner: Float, lit: Int, cross: Boolean, ink: Color, bg: Color) {
    val tile = (size.width - gap * (n - 1)) / n
    for (cell in 0 until n * n) {
        val o = Offset((cell % n) * (tile + gap), (cell / n) * (tile + gap))
        val r = CornerRadius(corner)
        if (cell == lit) {
            drawRoundRect(ink, o, Size(tile, tile), r)
            if (cross) {
                val m = tile * 0.3f
                drawLine(bg, o + Offset(m, m), o + Offset(tile - m, tile - m), stroke * 2)
                drawLine(bg, o + Offset(tile - m, m), o + Offset(m, tile - m), stroke * 2)
            } else {
                clipRect(o.x, o.y, o.x + tile, o.y + tile) {
                    drawCircle(bg, tile * 0.22f, o + Offset(tile * 0.96f, tile * 0.04f))
                }
            }
        } else {
            drawRoundRect(ink, o + Offset(stroke / 2, stroke / 2), Size(tile - stroke, tile - stroke), r, style = DrawStroke(stroke))
        }
    }
}

/**
 * Crumbs: crumbs pop up at random spots on the plate and vanish; tap [MissionKeys.Count] of
 * them. A crumb that vanishes and a tap on empty plate each cost one, so random tapping loses.
 */
object CrumbsMission : WakeMission {
    private val t = MissionTuning.Crumbs
    override val type = MissionType.Crumbs
    override val settings = listOf(
        MissionSetting(MissionKeys.Count, R.string.setting_crumbs, t.counts, t.DefaultCount),
        MissionSetting(MissionKeys.Level, R.string.setting_level, listOf(1, 2, 3), t.DefaultLevel, LevelLabels),
    )

    @Composable
    override fun Content(session: MissionSession, modifier: Modifier) {
        val c = Clookie.colors
        val haptics = rememberHaptics()
        val demo = LocalMissionDemo.current
        val density = LocalDensity.current
        val level = session.spec.param(MissionKeys.Level, t.DefaultLevel)
        val target = session.spec.param(MissionKeys.Count, t.DefaultCount).coerceAtLeast(1)
        val seed = rememberSaveable { if (demo) 7L else Random.nextLong() }
        val stroke = with(density) { Stroke.regular.toPx() }
        val corner = with(density) { Clookie.shapes.mediumRadius.toPx() }
        BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
            val plateHeight = minOf(maxHeight - 40.dp, t.MaxHeightDp.dp).coerceAtLeast(200.dp)
            val w = with(density) { maxWidth.toPx() }
            val h = with(density) { plateHeight.toPx() }
            val game = remember(w, h) { CrumbsGame(level, target, w, h, density.density, seed).also { if (demo) it.playDemo() } }
            var score by remember(game) { mutableIntStateOf(game.score) }
            val clock = remember(game) { mutableLongStateOf(if (demo) DemoClock else 0L) }
            LaunchedEffect(game) {
                if (demo) return@LaunchedEffect
                val start = withFrameMillis { it }
                while (!game.done) {
                    val now = withFrameMillis { it } - start
                    val before = game.score
                    game.update(now)
                    clock.longValue = now
                    if (game.score != before) {
                        haptics.reject()
                        score = game.score
                    }
                }
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Txt(
                    stringResource(R.string.crumbs_progress, score, target).uppercase(),
                    Clookie.type.eyebrow, color = c.onRinging, maxLines = 1,
                )
                Spacer(Modifier.height(Space.s3))
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(plateHeight)
                        .pointerInput(game) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                if (game.done) return@awaitEachGesture
                                session.interact()
                                val hit = game.tap(down.position.x, down.position.y, clock.longValue) == CrumbsGame.Tap.Hit
                                score = game.score
                                when {
                                    game.done -> {
                                        haptics.confirm()
                                        session.complete()
                                    }
                                    hit -> haptics.tick()
                                    else -> haptics.reject()
                                }
                            }
                        },
                ) {
                    val now = clock.longValue
                    val flash = game.lostAt >= 0 && now - game.lostAt < t.MissFlashMs
                    val s = if (flash) stroke * 2.5f else stroke
                    drawRoundRect(c.onRinging, Offset(s / 2, s / 2), Size(size.width - s, size.height - s), CornerRadius(corner), style = DrawStroke(s))
                    for (crumb in game.crumbs) drawCrumb(crumb, game, now, stroke, c.onRinging, c.ringing)
                }
            }
        }
    }

    private const val DemoClock = 4_000L

    /** Screenshots: a few crumbs eaten, two on the plate, one half gone. */
    private fun CrumbsGame.playDemo() {
        var now = 0L
        while (score < 6 && now < 20_000L) {
            update(now)
            crumbs.firstOrNull { it.bittenAt < 0 && now - it.bornMs > 200 }?.let { tap(it.x, it.y, now) }
            now += 100
        }
        while (crumbs.count { it.bittenAt < 0 } < 2 && now < DemoClock) {
            update(now)
            now += 50
        }
        update(DemoClock - 1)
    }
}

/** One crumb: a chip of three overlapping dots that pops in, a shrinking ring for the time left, a bite when eaten. */
private fun DrawScope.drawCrumb(crumb: CrumbsGame.Crumb, game: CrumbsGame, now: Long, stroke: Float, ink: Color, bg: Color) {
    val t = MissionTuning.Crumbs
    val age = now - crumb.bornMs
    val pop = (age.toFloat() / t.PopMs).coerceIn(0f, 1f)
    val bite = if (crumb.bittenAt >= 0) ((now - crumb.bittenAt).toFloat() / t.BiteMs).coerceIn(0f, 1f) else 0f
    val r = game.radius * (0.6f + 0.4f * pop) * (1f - 0.5f * bite)
    val o = Offset(crumb.x, crumb.y)
    val lobes = CrumbLobes[crumb.shape % CrumbLobes.size]
    for ((dx, dy, k) in lobes) drawCircle(ink, r * k, o + Offset(dx * r, dy * r))
    if (crumb.bittenAt >= 0) {
        drawCircle(bg, r * (0.5f + 0.5f * bite), o + Offset(r * 0.7f, -r * 0.6f))
    } else {
        val left = 1f - (age.toFloat() / game.lifeMs).coerceIn(0f, 1f)
        val ring = game.radius + stroke * 3
        drawArc(
            ink, -90f, 360f * left, useCenter = false,
            topLeft = o - Offset(ring, ring), size = Size(ring * 2, ring * 2), style = DrawStroke(stroke),
        )
    }
}

/** Four irregular crumb shapes: (dx, dy, radius factor) of each dot, in crumb radii. */
private val CrumbLobes = listOf(
    listOf(Triple(0f, 0f, 0.78f), Triple(0.35f, 0.3f, 0.55f), Triple(-0.38f, 0.2f, 0.45f)),
    listOf(Triple(0f, 0.05f, 0.8f), Triple(-0.3f, -0.35f, 0.5f), Triple(0.4f, -0.1f, 0.48f)),
    listOf(Triple(-0.1f, 0f, 0.74f), Triple(0.38f, 0.25f, 0.5f), Triple(0.15f, -0.4f, 0.42f)),
    listOf(Triple(0.05f, -0.05f, 0.76f), Triple(-0.35f, 0.32f, 0.52f), Triple(0.42f, 0.18f, 0.4f)),
)
