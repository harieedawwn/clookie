package io.github.harieedawwn.clookie.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.design.TimeSize
import io.github.harieedawwn.clookie.design.icons.ClookieIcon
import io.github.harieedawwn.clookie.design.icons.ClookieIcons
import io.github.harieedawwn.clookie.design.rememberHaptics
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Ringing screen layout: full-bleed solid color, giant time, a wobbling cookie, then the
 * mission slot (classic: [SlideToStop]) and the snooze slot. Missions plug into [mission].
 */
@Composable
fun RingingLayout(
    hour: Int,
    minute: Int,
    dateText: String,
    label: String,
    modifier: Modifier = Modifier,
    snooze: (@Composable () -> Unit)? = null,
    mission: @Composable ColumnScope.() -> Unit,
) {
    val c = Clookie.colors
    val reduced = Clookie.motion.reduced
    BoxWithConstraints(modifier.fillMaxSize().background(c.ringing).systemBarsPadding()) {
        // Short screens get smaller numerals so a camera or keypad mission still fits.
        val short = maxHeight < 720.dp
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = Space.gutter, vertical = if (short) Space.s4 else Space.s6),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Txt(dateText.uppercase(), Clookie.type.eyebrow, color = c.onRinging)
            Spacer(Modifier.height(Space.s2))
            TimeDisplay(hour, minute, if (short) TimeSize.Ringing.dp * 0.7f else TimeSize.Ringing.dp, color = c.onRinging)
            if (label.isNotEmpty()) {
                Spacer(Modifier.height(Space.s2))
                Txt(label, Clookie.type.title, color = c.onRinging, align = TextAlign.Center, maxLines = if (short) 1 else 2)
            }
            // Mission and cookie share what is left: the mission is measured first (it may size
            // itself to the height it gets), the cookie takes the rest or disappears.
            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val mark = minOf(132.dp, maxHeight * 0.85f)
                    if (mark < 40.dp) {
                        // No room: keep the time and the mission.
                    } else if (reduced) {
                        ClookieMark(size = mark, cookie = c.onRinging, chips = c.ringing)
                    } else {
                        val wobble = rememberInfiniteTransition(label = "wobble")
                        val angle = wobble.animateFloat(-6f, 6f, infiniteRepeatable(tween(380), RepeatMode.Reverse), label = "a")
                        ClookieMark(
                            Modifier.graphicsLayer { rotationZ = angle.value },
                            size = mark,
                            cookie = c.onRinging,
                            chips = c.ringing,
                        )
                    }
                }
                mission()
            }
            if (snooze != null) {
                Spacer(Modifier.height(Space.s4))
                snooze()
            }
        }
    }
}

/** Classic mission: drag the cookie across the track to stop the alarm. [onDrag] fires on each drag start. */
@Composable
fun SlideToStop(label: String, onDone: () -> Unit, modifier: Modifier = Modifier, onDrag: () -> Unit = {}) {
    val c = Clookie.colors
    val motion = Clookie.motion
    val haptics = rememberHaptics()
    val offset = remember { Animatable(0f) }
    var drag by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val crisp = Clookie.shapes.smallRadius < 6.dp
    val trackShape = if (crisp) RoundedCornerShape(6.dp) else CircleShape
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(trackShape)
            .border(Stroke.regular, c.onRinging, trackShape)
            .semantics {
                role = Role.Button
                contentDescription = label
                onClick { onDone(); true }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val thumb = 64.dp
        val maxPx = with(density) { (maxWidth - thumb - 12.dp).toPx() }
        Txt(
            label, Clookie.type.heading, color = c.onRinging, align = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = thumb)
                .graphicsLayer { alpha = 1f - (offset.value / maxPx).coerceIn(0f, 1f) },
        )
        Box(
            Modifier
                .padding(6.dp)
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .size(thumb)
                .clip(trackShape)
                .background(c.onRinging)
                .draggable(
                    // The thumb follows the finger exactly: deltas are applied synchronously (one
                    // coroutine per delta used to cancel the previous snap, so the thumb lagged and
                    // a full swipe often fell short of the end).
                    rememberDraggableState { d ->
                        drag = (drag + d).coerceIn(0f, maxPx)
                        scope.launch { offset.snapTo(drag) }
                    },
                    Orientation.Horizontal,
                    onDragStarted = {
                        offset.stop()
                        drag = offset.value
                        onDrag()
                    },
                    onDragStopped = { velocity ->
                        // Past 60 %, or a quick flick to the right past 25 %, stops the alarm.
                        val flick = velocity > with(density) { 900.dp.toPx() } && drag > maxPx * 0.25f
                        if (drag > maxPx * 0.6f || flick) {
                            drag = maxPx
                            haptics.confirm()
                            onDone()
                            offset.animateTo(maxPx, motion.snappy())
                        } else {
                            drag = 0f
                            offset.animateTo(0f, motion.playful())
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            ClookieIcon(ClookieIcons.Chevron, null, tint = c.ringing, size = 28.dp)
        }
    }
}

/**
 * Dismissal bite: three bites chew the screen away from [origin] (fraction of the size) as
 * [progress] goes 0 -> 1. Pure draw-phase work on an offscreen layer.
 */
fun Modifier.biteAway(progress: () -> Float, origin: Offset = Offset(0.85f, 0.85f)): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val p = progress()
        if (p <= 0f) return@drawWithContent
        val o = Offset(size.width * origin.x, size.height * origin.y)
        val far = hypot(size.width, size.height)
        val first = (p / 0.35f).coerceAtMost(1f)
        val second = ((p - 0.25f) / 0.35f).coerceIn(0f, 1f)
        val last = ((p - 0.5f) / 0.5f).coerceIn(0f, 1f)
        drawCircle(Color.Black, size.minDimension * 0.22f * first, o, blendMode = BlendMode.Clear)
        drawCircle(Color.Black, size.minDimension * 0.3f * second, o + Offset(-size.minDimension * 0.28f, -size.minDimension * 0.18f), blendMode = BlendMode.Clear)
        drawCircle(Color.Black, far * last, o, blendMode = BlendMode.Clear)
    }

/** Outline pill on the ringing color, used for snooze. */
@Composable
fun RingingPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Clookie.colors
    val shape = pillShape()
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .pressable(shape, onClick, shadow = false)
            .border(Stroke.regular, c.onRinging, shape),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            ClookieIcon(ClookieIcons.Snooze, null, tint = c.onRinging, size = 22.dp)
            Txt(text, Clookie.type.heading, color = c.onRinging)
        }
    }
}

/** Outline chip on the ringing color (mission controls); [selected] fills it. */
@Composable
fun RingingChip(
    text: String?,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    contentDescription: String? = null,
) {
    val c = Clookie.colors
    val shape = pillShape()
    val fg = if (selected) c.ringing else c.onRinging
    Row(
        modifier
            .heightIn(min = 48.dp)
            .pressable(shape, onClick, shadow = false)
            .background(if (selected) c.onRinging else Color.Transparent)
            .border(Stroke.regular, c.onRinging, shape)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .padding(horizontal = if (text == null) Space.s3 else Space.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) ClookieIcon(icon, null, tint = fg, size = 22.dp)
        if (icon != null && text != null) Spacer(Modifier.width(Space.s2))
        if (text != null) Txt(text, Clookie.type.bodyStrong, color = fg, maxLines = 1)
    }
}

/** Digits 0-9, backspace and OK on the ringing color, for typed answers. */
@Composable
fun RingingNumberPad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
    backspaceLabel: String,
    okLabel: String,
    keyHeight: Dp = 52.dp,
) {
    val c = Clookie.colors
    val haptics = rememberHaptics()
    val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9), listOf(-1, 0, -2))
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.s2)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                row.forEach { key ->
                    val shape = Clookie.shapes.medium
                    val filled = key == -2
                    Box(
                        Modifier
                            .weight(1f)
                            .height(keyHeight)
                            .pressable(shape, {
                                when (key) {
                                    -1 -> onBackspace()
                                    -2 -> onOk()
                                    else -> {
                                        haptics.tick()
                                        onDigit(key)
                                    }
                                }
                            }, shadow = false)
                            .background(if (filled) c.onRinging else Color.Transparent)
                            .border(Stroke.thin, c.onRinging, shape)
                            .semantics {
                                contentDescription = when (key) {
                                    -1 -> backspaceLabel
                                    -2 -> okLabel
                                    else -> key.toString()
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        when (key) {
                            -1 -> ClookieIcon(ClookieIcons.Backspace, null, tint = c.onRinging)
                            -2 -> ClookieIcon(ClookieIcons.Check, null, tint = c.ringing)
                            else -> Txt(key.toString(), Clookie.type.title, color = c.onRinging)
                        }
                    }
                }
            }
        }
    }
}
