package io.github.harieedawwn.clookie.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.hardShadow
import io.github.harieedawwn.clookie.design.icons.ClookieIcon
import io.github.harieedawwn.clookie.design.icons.ClookieIcons
import io.github.harieedawwn.clookie.design.rememberHaptics

enum class NavDest(val icon: ImageVector, @StringRes val label: Int) {
    Alarms(ClookieIcons.Alarm, R.string.nav_alarms),
    Timer(ClookieIcons.Timer, R.string.nav_timer),
    Stopwatch(ClookieIcons.Stopwatch, R.string.nav_stopwatch),
    Settings(ClookieIcons.Settings, R.string.nav_settings),
}

/**
 * Floating ink bar. The selected destination grows into a soft "dough" pill with its label;
 * the others are icons only.
 */
@Composable
fun ClookieBottomNav(selected: NavDest, onSelect: (NavDest) -> Unit, modifier: Modifier = Modifier) {
    val c = Clookie.colors
    val shape = Clookie.shapes.large
    val haptics = rememberHaptics()
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Space.s4, vertical = Space.s3)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .hardShadow(shape)
                .clip(shape)
                .background(c.ink)
                .padding(Space.s2),
            horizontalArrangement = Arrangement.spacedBy(Space.s1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavDest.entries.forEach { dest ->
                val isSel = dest == selected
                val weight by animateFloatAsState(if (isSel) 2.2f else 1f, Clookie.motion.standard(), label = "w")
                val label = stringResource(dest.label)
                Row(
                    Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .clip(pillShape())
                        .background(if (isSel) c.accentSoft else c.ink)
                        .clickable(role = Role.Tab) {
                            if (!isSel) haptics.tick()
                            onSelect(dest)
                        }
                        .semantics {
                            this.selected = isSel
                            contentDescription = label
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ClookieIcon(dest.icon, null, tint = if (isSel) c.onAccentSoft else c.background, size = 22.dp)
                    if (isSel) {
                        Spacer(Modifier.width(Space.s2))
                        Txt(label, Clookie.type.label, color = c.onAccentSoft, maxLines = 1)
                    }
                }
            }
        }
    }
}
