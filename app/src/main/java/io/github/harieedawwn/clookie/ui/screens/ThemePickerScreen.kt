package io.github.harieedawwn.clookie.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.ClookieTheme
import io.github.harieedawwn.clookie.design.Preset
import io.github.harieedawwn.clookie.design.Presets
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.design.hardShadow
import io.github.harieedawwn.clookie.design.icons.ClookieIcons
import io.github.harieedawwn.clookie.design.resolvePreset
import io.github.harieedawwn.clookie.model.Alarm
import io.github.harieedawwn.clookie.ui.TopBar
import io.github.harieedawwn.clookie.ui.components.ButtonKind
import io.github.harieedawwn.clookie.ui.components.CookieButton
import io.github.harieedawwn.clookie.ui.components.IconCircleButton
import io.github.harieedawwn.clookie.ui.components.Txt
import io.github.harieedawwn.clookie.ui.components.drawClookieMark
import io.github.harieedawwn.clookie.ui.components.pressable
import io.github.harieedawwn.clookie.ui.screenBackground

/** Theme picker: a live, scaled-down Home in the candidate preset, preset swatches, apply. */
@Composable
fun ThemePickerScreen(
    current: Preset,
    alarms: List<Alarm>,
    onApply: (Preset) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pick by remember { mutableStateOf(current) }
    Column(
        modifier.fillMaxSize().screenBackground().statusBarsPadding().navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopBar(
            stringResource(R.string.appearance),
            Modifier.padding(horizontal = Space.gutter),
            leading = { IconCircleButton(ClookieIcons.Back, stringResource(R.string.action_back), onBack) },
        )
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(vertical = Space.s3), contentAlignment = Alignment.Center) {
            val scale = minOf(maxHeight / 760.dp, maxWidth / 412.dp * 0.62f, 0.6f)
            LivePreview(pick, alarms, scale)
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = Space.gutter),
            horizontalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            items(Presets.all, key = { it.id }) { p ->
                Swatch(p, selected = p.id == pick.id, onClick = { pick = p })
            }
        }
        Spacer(Modifier.height(Space.s4))
        CookieButton(
            stringResource(if (pick.id == current.id) R.string.theme_in_use else R.string.theme_apply),
            onClick = { onApply(pick) },
            enabled = pick.id != current.id,
            kind = if (pick.id == current.id) ButtonKind.Secondary else ButtonKind.Primary,
            icon = ClookieIcons.Check,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.gutter),
        )
        Spacer(Modifier.height(Space.s4))
    }
}

/** Real Home composables rendered at phone size, then scaled; input is swallowed. */
@Composable
private fun LivePreview(preset: Preset, alarms: List<Alarm>, scale: Float) {
    val w = 412.dp
    val h = 760.dp
    val shape = RoundedCornerShape(28.dp * scale + 6.dp)
    Box(
        Modifier
            .size(w * scale, h * scale)
            .hardShadow(shape)
            .clip(shape)
            .border(Stroke.regular, Clookie.colors.outline, shape)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            },
    ) {
        Box(
            Modifier.layout { measurable, _ ->
                val p = measurable.measure(Constraints.fixed(w.roundToPx(), h.roundToPx()))
                layout((w * scale).roundToPx(), (h * scale).roundToPx()) {
                    p.placeWithLayer(0, 0) {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                }
            },
        ) {
            ClookieTheme(preset, reducedMotion = true) {
                HomeChrome(alarms = alarms, reliabilityIssue = false)
            }
        }
    }
}

@Composable
private fun Swatch(preset: Preset, selected: Boolean, onClick: () -> Unit) {
    val p = resolvePreset(preset).colors
    val c = Clookie.colors
    val name = stringResource(preset.nameRes)
    val path = remember { Path() }
    Column(
        Modifier
            .width(84.dp)
            .semantics {
                this.selected = selected
                contentDescription = name
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(76.dp)
                .pressable(CircleShape, onClick, shadow = false, role = Role.RadioButton)
                .border(if (selected) 4.dp else Stroke.thin, if (selected) c.ink else c.outline, CircleShape)
                .padding(if (selected) 6.dp else 4.dp)
                .clip(CircleShape)
                .background(p.background),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(44.dp)) {
                drawClookieMark(p.accent, p.onAccent, Offset.Zero, size.minDimension, path)
                if (p.hasGrid) drawCircle(p.margin, size.minDimension * 0.62f, style = DrawStroke(1.dp.toPx()))
            }
        }
        Spacer(Modifier.height(Space.s2))
        Txt(name, Clookie.type.label, align = TextAlign.Center, maxLines = 2, color = if (selected) c.ink else c.inkMuted)
    }
}
