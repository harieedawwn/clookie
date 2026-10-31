package io.github.harieedawwn.clookie.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.design.AccentSwatches
import io.github.harieedawwn.clookie.design.ClockFont
import io.github.harieedawwn.clookie.design.ClockStyle
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.ClookieColors
import io.github.harieedawwn.clookie.design.ClookieTheme
import io.github.harieedawwn.clookie.design.CornerStyle
import io.github.harieedawwn.clookie.design.CustomPreset
import io.github.harieedawwn.clookie.design.Look
import io.github.harieedawwn.clookie.design.Presets
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.design.UiDensity
import io.github.harieedawwn.clookie.design.adjustAccent
import io.github.harieedawwn.clookie.design.hardShadow
import io.github.harieedawwn.clookie.design.icons.ClookieIcon
import io.github.harieedawwn.clookie.design.icons.ClookieIcons
import io.github.harieedawwn.clookie.design.resolvePreset
import io.github.harieedawwn.clookie.design.with
import io.github.harieedawwn.clookie.model.Alarm
import io.github.harieedawwn.clookie.ui.Eyebrow
import io.github.harieedawwn.clookie.ui.TopBar
import io.github.harieedawwn.clookie.ui.components.ButtonKind
import io.github.harieedawwn.clookie.ui.components.CookieButton
import io.github.harieedawwn.clookie.ui.components.IconCircleButton
import io.github.harieedawwn.clookie.ui.components.Segmented
import io.github.harieedawwn.clookie.ui.components.Txt
import io.github.harieedawwn.clookie.ui.components.drawClookieMark
import io.github.harieedawwn.clookie.ui.components.pressable
import io.github.harieedawwn.clookie.ui.screenBackground

/** What the studio is editing: a look, the home clock face and (when picked) the saved preset it came from. */
data class StudioDraft(val look: Look, val clockStyle: ClockStyle, val customId: String? = null)

/**
 * Customization studio: a live, scaled-down Home in the draft look on top; presets (built-in
 * and saved), accent, clock face, numerals, corners, hard shadow, density and motion below.
 * A failing accent is auto-adjusted and both swatches are shown.
 */
@Composable
fun StudioScreen(
    current: Look,
    currentStyle: ClockStyle,
    customPresets: List<CustomPreset>,
    reducedMotion: Boolean,
    alarms: List<Alarm>,
    onApply: (Look, ClockStyle) -> Unit,
    onSave: (Look, ClockStyle) -> Unit,
    onDeleteCustom: (String) -> Unit,
    onReducedMotion: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initial: StudioDraft? = null,
) {
    var draft by remember { mutableStateOf(initial ?: StudioDraft(current, currentStyle, customPresets.firstOrNull { it.look == current }?.id)) }
    val inUse = draft.look == current && draft.clockStyle == currentStyle
    Column(modifier.fillMaxSize().screenBackground().statusBarsPadding().navigationBarsPadding()) {
        TopBar(
            stringResource(R.string.studio),
            Modifier.padding(horizontal = Space.gutter),
            leading = { IconCircleButton(ClookieIcons.Back, stringResource(R.string.action_back), onBack) },
        )
        Box(Modifier.fillMaxWidth().padding(vertical = Space.s2), contentAlignment = Alignment.Center) {
            LivePreview(draft.look, draft.clockStyle, alarms, height = 292.dp)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = Space.s3),
            verticalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            PresetRow(draft, customPresets, onPick = { draft = it }, onDelete = {
                onDeleteCustom(it)
                draft = draft.copy(customId = null)
            })
            Section(R.string.accent) { AccentPicker(draft.look) { draft = draft.copy(look = draft.look.copy(accent = it), customId = null) } }
            Section(R.string.clock_style) {
                ChoiceChips(
                    ClockStyle.entries.map { it to stringResource(it.nameRes) }, draft.clockStyle,
                    { draft = draft.copy(clockStyle = it, customId = null) },
                )
            }
            Section(R.string.clock_font) {
                Segmented(
                    listOf(ClockFont.Display to stringResource(R.string.font_display), ClockFont.Ui to stringResource(R.string.font_ui)), draft.look.clockFont,
                    { draft = draft.copy(look = draft.look.copy(clockFont = it), customId = null) }, Modifier.fillMaxWidth(),
                )
            }
            val base = Presets.byId(draft.look.base)
            Section(R.string.corners) {
                Segmented(
                    listOf(
                        CornerStyle.Soft to stringResource(R.string.corner_soft),
                        CornerStyle.Rounded to stringResource(R.string.corner_rounded),
                        CornerStyle.Crisp to stringResource(R.string.corner_crisp),
                    ),
                    draft.look.corners ?: base.corners,
                    { draft = draft.copy(look = draft.look.copy(corners = it.takeIf { c -> c != base.corners }), customId = null) },
                    Modifier.fillMaxWidth(),
                )
            }
            Section(R.string.hard_shadow) {
                Segmented(
                    listOf(true to stringResource(R.string.on), false to stringResource(R.string.off)),
                    draft.look.hardShadow ?: base.hardShadow,
                    { draft = draft.copy(look = draft.look.copy(hardShadow = it.takeIf { h -> h != base.hardShadow }), customId = null) },
                    Modifier.fillMaxWidth(),
                )
            }
            Section(R.string.density) {
                Segmented(
                    listOf(UiDensity.Comfortable to stringResource(R.string.density_comfortable), UiDensity.Compact to stringResource(R.string.density_compact)),
                    draft.look.density,
                    { draft = draft.copy(look = draft.look.copy(density = it), customId = null) },
                    Modifier.fillMaxWidth(),
                )
            }
            Section(R.string.motion) {
                Segmented(
                    listOf(false to stringResource(R.string.motion_full), true to stringResource(R.string.motion_reduced)),
                    reducedMotion, onReducedMotion, Modifier.fillMaxWidth(),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = Space.s3), horizontalArrangement = Arrangement.spacedBy(Space.s3)) {
            CookieButton(
                stringResource(R.string.save_preset), { onSave(draft.look, draft.clockStyle) },
                kind = ButtonKind.Secondary, icon = ClookieIcons.Plus, enabled = draft.customId == null && !draft.look.isPlain,
                modifier = Modifier.weight(1f),
            )
            CookieButton(
                stringResource(if (inUse) R.string.theme_in_use else R.string.studio_apply), { onApply(draft.look, draft.clockStyle) },
                kind = if (inUse) ButtonKind.Secondary else ButtonKind.Primary, icon = ClookieIcons.Check, enabled = !inUse,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

val ClockStyle.nameRes: Int
    get() = when (this) {
        ClockStyle.Digital -> R.string.style_digital
        ClockStyle.Cookie -> R.string.style_cookie
        ClockStyle.Words -> R.string.style_words
        ClockStyle.Minimal -> R.string.style_minimal
    }

@Composable
private fun Section(title: Int, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = Space.gutter)) {
        Eyebrow(stringResource(title))
        Spacer(Modifier.height(Space.s2))
        content()
    }
}

@Composable
private fun PresetRow(draft: StudioDraft, custom: List<CustomPreset>, onPick: (StudioDraft) -> Unit, onDelete: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Space.gutter), verticalAlignment = Alignment.CenterVertically) {
        Eyebrow(stringResource(R.string.theme), Modifier.weight(1f))
        val sel = draft.customId
        if (sel != null) IconCircleButton(ClookieIcons.Trash, stringResource(R.string.action_delete), { onDelete(sel) }, size = 40.dp)
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = Space.gutter),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        items(custom, key = { it.id }) { p ->
            val colors = resolvePreset(Presets.byId(p.look.base)).with(p.look).colors
            Swatch(colors, p.name, selected = draft.customId == p.id, onClick = { onPick(StudioDraft(p.look, draft.clockStyle, p.id)) })
        }
        items(Presets.all, key = { it.id }) { p ->
            Swatch(
                resolvePreset(p).colors, stringResource(p.nameRes),
                selected = draft.customId == null && draft.look.base == p.id && draft.look.isPlain,
                onClick = { onPick(StudioDraft(Look(p.id), draft.clockStyle)) },
            )
        }
    }
}

/** Accent dots; "auto" keeps the preset's own accent. A failing pick shows picked -> adjusted. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentPicker(look: Look, onPick: (Color?) -> Unit) {
    val base = resolvePreset(Presets.byId(look.base)).colors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s2), verticalArrangement = Arrangement.spacedBy(Space.s2)) {
        AccentDot(base.accent, selected = look.accent == null, description = stringResource(R.string.accent_auto), auto = true) { onPick(null) }
        AccentSwatches.forEach { col -> AccentDot(col, selected = look.accent == col, description = null) { onPick(col) } }
    }
    val picked = look.accent ?: return
    val adjusted = adjustAccent(picked, base)
    if (adjusted != picked) {
        Spacer(Modifier.height(Space.s3))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(picked, 28.dp)
            ClookieIcon(ClookieIcons.Chevron, null, tint = Clookie.colors.inkMuted, size = 18.dp, modifier = Modifier.padding(horizontal = Space.s1))
            Dot(adjusted, 28.dp)
            Spacer(Modifier.width(Space.s3))
            Txt(stringResource(R.string.contrast_adjusted), Clookie.type.label, color = Clookie.colors.inkMuted, maxLines = 1)
        }
    }
}

@Composable
private fun Dot(color: Color, size: Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color).border(Stroke.thin, Clookie.colors.outline, CircleShape))
}

@Composable
private fun AccentDot(color: Color, selected: Boolean, description: String?, auto: Boolean = false, onClick: () -> Unit) {
    val c = Clookie.colors
    Box(
        Modifier
            .size(44.dp)
            .semantics {
                this.selected = selected
                if (description != null) contentDescription = description
            }
            .pressable(CircleShape, onClick, shadow = false, role = Role.RadioButton)
            .border(if (selected) 3.dp else Stroke.thin, if (selected) c.ink else c.outline, CircleShape)
            .padding(if (selected) 6.dp else 4.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        if (auto) ClookieIcon(ClookieIcons.Cookie, null, size = 18.dp, tint = if (contrast(Color.White, color) > contrast(Color.Black, color)) Color.White else Color.Black)
    }
}

private fun contrast(a: Color, b: Color) = io.github.harieedawwn.clookie.design.contrast(a, b)

/** Real Home composables rendered at phone size, then scaled; input is swallowed. */
@Composable
private fun LivePreview(look: Look, style: ClockStyle, alarms: List<Alarm>, height: Dp) {
    val w = 412.dp
    val h = 760.dp
    val scale = height / h
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
            ClookieTheme(Presets.byId(look.base), reducedMotion = true, look = look) {
                HomeChrome(alarms = alarms, reliabilityIssue = false, clockStyle = style)
            }
        }
    }
}

@Composable
private fun Swatch(p: ClookieColors, name: String, selected: Boolean, onClick: () -> Unit) {
    val c = Clookie.colors
    val path = remember { Path() }
    Column(
        Modifier
            .width(76.dp)
            .semantics {
                this.selected = selected
                contentDescription = name
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .pressable(CircleShape, onClick, shadow = false, role = Role.RadioButton)
                .border(if (selected) 4.dp else Stroke.thin, if (selected) c.ink else c.outline, CircleShape)
                .padding(if (selected) 6.dp else 4.dp)
                .clip(CircleShape)
                .background(p.background),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(36.dp)) {
                drawClookieMark(p.accent, p.onAccent, Offset.Zero, size.minDimension, path)
                if (p.hasGrid) drawCircle(p.margin, size.minDimension * 0.62f, style = DrawStroke(1.dp.toPx()))
            }
        }
        Spacer(Modifier.height(Space.s1))
        Txt(name, Clookie.type.caption, align = TextAlign.Center, maxLines = 1, color = if (selected) c.ink else c.inkMuted)
    }
}
