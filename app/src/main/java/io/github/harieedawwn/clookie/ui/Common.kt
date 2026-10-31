package io.github.harieedawwn.clookie.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.design.icons.ClookieIcon
import io.github.harieedawwn.clookie.design.icons.ClookieIcons
import io.github.harieedawwn.clookie.design.paperGrid
import io.github.harieedawwn.clookie.ui.components.Txt
import io.github.harieedawwn.clookie.ui.components.pressable
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/**
 * Wall clock, ticking on the second boundary. Read `.value` only in small composables (or
 * derivedStateOf) so at most those recompose, at most once per second.
 */
val LocalNow = staticCompositionLocalOf<State<LocalDateTime>> { mutableStateOf(LocalDateTime.now()) }

@Composable
fun rememberTicker(): State<LocalDateTime> {
    val state = remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000 - System.currentTimeMillis() % 1000)
            state.value = LocalDateTime.now()
        }
    }
    return state
}

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimatedScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** Slab <-> editor shared bounds; a no-op outside the app's SharedTransitionLayout (tests). */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedAlarm(id: Long): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val animated = LocalAnimatedScope.current ?: return this
    if (Clookie.motion.reduced) return this
    return with(shared) {
        this@sharedAlarm.sharedBounds(
            rememberSharedContentState("alarm-$id"), animated,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
        )
    }
}

/** Solid screen color, or the printed grid for Giấy ô li. */
@Composable
fun Modifier.screenBackground(): Modifier {
    val c = Clookie.colors
    return background(c.background).paperGrid(c)
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Txt(text.uppercase(), Clookie.type.eyebrow, modifier, color = Clookie.colors.inkMuted)
}

/** Top bar: optional leading/trailing round buttons, centered title. */
@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Box(modifier.fillMaxWidth().heightIn(min = 56.dp), contentAlignment = Alignment.Center) {
        Txt(title, Clookie.type.heading, align = TextAlign.Center, maxLines = 1, modifier = Modifier.padding(horizontal = 64.dp))
        if (leading != null) Box(Modifier.align(Alignment.CenterStart)) { leading() }
        if (trailing != null) Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
    }
}

/** Settings/editor block: icon chip, title, value; not a stock list row. */
@Composable
fun Tile(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    trailing: @Composable (() -> Unit)? = null,
) {
    val c = Clookie.colors
    val shape = Clookie.shapes.medium
    Column(
        modifier
            .heightIn(min = 108.dp)
            .pressable(shape, onClick, shadow = false)
            .background(c.surface)
            .border(Stroke.thin, c.outline, shape)
            .padding(Space.s4),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            IconChip(icon)
            Spacer(Modifier.weight(1f))
            trailing?.invoke()
        }
        Spacer(Modifier.size(Space.s3))
        Txt(title, Clookie.type.label, color = c.inkMuted, maxLines = 1)
        Txt(value, Clookie.type.bodyStrong, maxLines = 2)
    }
}

@Composable
fun IconChip(icon: ImageVector, modifier: Modifier = Modifier) {
    val c = Clookie.colors
    Box(
        modifier.size(36.dp).background(c.accentSoft, Clookie.shapes.small),
        contentAlignment = Alignment.Center,
    ) { ClookieIcon(icon, null, tint = c.onAccentSoft, size = 20.dp) }
}

/** Full-width settings row inside a grouped card: icon chip, title, value, chevron. */
@Composable
fun SettingsRow(icon: ImageVector, title: String, value: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Clookie.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .pressable(androidx.compose.ui.graphics.RectangleShape, onClick, shadow = false)
            .padding(horizontal = Space.s4, vertical = Space.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconChip(icon)
        Spacer(Modifier.width(Space.s3))
        Column(Modifier.weight(1f)) {
            Txt(title, Clookie.type.bodyStrong, maxLines = 1)
            if (value != null) Txt(value, Clookie.type.label, color = c.inkMuted, maxLines = 1)
        }
        ClookieIcon(ClookieIcons.Chevron, null, tint = c.inkMuted, size = 20.dp)
    }
}
