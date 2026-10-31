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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
    Column(
        modifier
            .fillMaxSize()
            .background(c.ringing)
            .systemBarsPadding()
            .padding(horizontal = Space.gutter, vertical = Space.s6),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Txt(dateText.uppercase(), Clookie.type.eyebrow, color = c.onRinging)
        Spacer(Modifier.height(Space.s2))
        TimeDisplay(hour, minute, TimeSize.Ringing.dp, Modifier.fillMaxWidth(), color = c.onRinging)
        if (label.isNotEmpty()) {
            Spacer(Modifier.height(Space.s2))
            Txt(label, Clookie.type.title, color = c.onRinging, align = TextAlign.Center, maxLines = 2)
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (reduced) {
                ClookieMark(size = 132.dp, cookie = c.onRinging, chips = c.ringing)
            } else {
                val wobble = rememberInfiniteTransition(label = "wobble")
                val angle = wobble.animateFloat(-6f, 6f, infiniteRepeatable(tween(380), RepeatMode.Reverse), label = "a")
                ClookieMark(
                    Modifier.graphicsLayer { rotationZ = angle.value },
                    size = 132.dp,
                    cookie = c.onRinging,
                    chips = c.ringing,
                )
            }
        }
        mission()
        if (snooze != null) {
            Spacer(Modifier.height(Space.s4))
            snooze()
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
                    rememberDraggableState { d -> scope.launch { offset.snapTo((offset.value + d).coerceIn(0f, maxPx)) } },
                    Orientation.Horizontal,
                    onDragStarted = { onDrag() },
                    onDragStopped = {
                        if (offset.value > maxPx * 0.82f) {
                            offset.animateTo(maxPx, motion.snappy())
                            haptics.confirm()
                            onDone()
                        } else {
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
