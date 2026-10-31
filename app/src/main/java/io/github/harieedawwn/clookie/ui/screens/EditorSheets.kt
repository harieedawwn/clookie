package io.github.harieedawwn.clookie.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.alarm.BuiltInSound
import io.github.harieedawwn.clookie.alarm.SoundPreview
import io.github.harieedawwn.clookie.alarm.SoundRef
import io.github.harieedawwn.clookie.alarm.Sounds
import io.github.harieedawwn.clookie.alarm.SystemTone
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.design.icons.ClookieIcon
import io.github.harieedawwn.clookie.design.icons.ClookieIcons
import io.github.harieedawwn.clookie.mission.MissionRegistry
import io.github.harieedawwn.clookie.model.Alarm
import io.github.harieedawwn.clookie.model.MissionSpec
import io.github.harieedawwn.clookie.model.VibrationPattern
import io.github.harieedawwn.clookie.ui.Eyebrow
import io.github.harieedawwn.clookie.ui.components.CookieButton
import io.github.harieedawwn.clookie.ui.components.IconCircleButton
import io.github.harieedawwn.clookie.ui.components.PillChip
import io.github.harieedawwn.clookie.ui.components.Txt
import io.github.harieedawwn.clookie.ui.components.icon
import io.github.harieedawwn.clookie.ui.components.nameRes
import io.github.harieedawwn.clookie.ui.components.pressable

enum class EditorSheet { Keypad, Label, Sound, Volume, Vibration, Snooze, Missions }

private val Volumes = listOf(20, 40, 60, 80, 100)
private val Ramps = listOf(0, 10, 20, 60, 120)
private val SnoozeLengths = listOf(5, 9, 10, 15, 20)
private val SnoozeLimits = listOf(0, 1, 2, 3, 5, 10)

/** Chips for one choice, wrapping to lines; the selected chip is ink-filled. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.s2),
        verticalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        options.forEach { (value, label) -> PillChip(label, selected = value == selected, onClick = { onSelect(value) }) }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Txt(text, Clookie.type.title, Modifier.fillMaxWidth())
    Spacer(Modifier.height(Space.s4))
}

@Composable
fun LabelSheet(label: String, onDone: (String) -> Unit) {
    val c = Clookie.colors
    var text by remember { mutableStateOf(label) }
    val focus = remember { FocusRequester() }
    val inspection = LocalInspectionMode.current
    LaunchedEffect(Unit) { if (!inspection) runCatching { focus.requestFocus() } }
    Column(Modifier.fillMaxWidth().imePadding()) {
        SheetTitle(stringResource(R.string.label))
        BasicTextField(
            value = text,
            onValueChange = { text = it.take(40) },
            singleLine = true,
            textStyle = Clookie.type.title.merge(color = c.ink),
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone(text.trim()) }),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            decorationBox = { inner ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .border(Stroke.regular, c.outline, Clookie.shapes.medium)
                        .padding(horizontal = Space.s4, vertical = Space.s3),
                ) {
                    if (text.isEmpty()) Txt(stringResource(R.string.label_hint), Clookie.type.title, color = c.inkMuted)
                    inner()
                }
            },
        )
        Spacer(Modifier.height(Space.s4))
        CookieButton(stringResource(R.string.action_done), { onDone(text.trim()) }, Modifier.fillMaxWidth(), icon = ClookieIcons.Check)
        Spacer(Modifier.height(Space.s3))
    }
}

/** Six Clookie sounds, the system's alarm tones and the user's own file; tapping previews. */
@Composable
fun SoundSheet(selected: String, onSelect: (String) -> Unit) {
    val context = LocalContext.current
    val c = Clookie.colors
    val inspection = LocalInspectionMode.current
    DisposableEffect(Unit) { onDispose { SoundPreview.stop() } }
    val tones by produceState(emptyList<SystemTone>()) { if (!inspection) value = Sounds.systemTones(context) }
    fun pick(id: String) {
        onSelect(id)
        if (!inspection) SoundPreview.play(context, id)
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            pick(SoundRef.File(uri).id)
        }
    }
    val current = SoundRef.parse(selected)
    Column(Modifier.fillMaxWidth()) {
        SheetTitle(stringResource(R.string.sound))
        Eyebrow(stringResource(R.string.app_name))
        Spacer(Modifier.height(Space.s2))
        ChoiceChips(
            BuiltInSound.entries.map { SoundRef.BuiltIn(it).id to stringResource(it.nameRes) },
            selected = if (current is SoundRef.BuiltIn) current.id else "",
            onSelect = ::pick,
        )
        if (tones.isNotEmpty()) {
            Spacer(Modifier.height(Space.s4))
            Eyebrow(stringResource(R.string.sound_group_system))
            Spacer(Modifier.height(Space.s2))
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .border(Stroke.thin, c.outline, Clookie.shapes.medium)
                    .verticalScroll(rememberScrollState()),
            ) {
                tones.forEach { tone ->
                    val id = SoundRef.System(tone.uri).id
                    ToneRow(tone.title, id == selected) { pick(id) }
                }
            }
        }
        Spacer(Modifier.height(Space.s4))
        val fileName by produceState<String?>(null, selected) {
            value = if (current is SoundRef.File && !inspection) Sounds.displayName(context, selected) else null
        }
        PillChip(
            fileName ?: stringResource(R.string.sound_pick_file),
            selected = current is SoundRef.File,
            onClick = { picker.launch(arrayOf("audio/*")) },
            icon = ClookieIcons.Plus,
        )
        Spacer(Modifier.height(Space.s3))
    }
}

@Composable
private fun ToneRow(title: String, selected: Boolean, onClick: () -> Unit) {
    val c = Clookie.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .pressable(androidx.compose.ui.graphics.RectangleShape, onClick, shadow = false)
            .padding(horizontal = Space.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(title, Clookie.type.body, Modifier.weight(1f), maxLines = 1)
        if (selected) ClookieIcon(ClookieIcons.Check, null, tint = c.accent, size = 20.dp)
    }
}

@Composable
fun VolumeSheet(alarm: Alarm, onChange: (Alarm) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        SheetTitle(stringResource(R.string.volume))
        Eyebrow(stringResource(R.string.volume_level))
        Spacer(Modifier.height(Space.s2))
        VolumeBlocks(alarm.volume) { onChange(alarm.copy(volume = it)) }
        Spacer(Modifier.height(Space.s4))
        Eyebrow(stringResource(R.string.ramp))
        Spacer(Modifier.height(Space.s2))
        ChoiceChips(
            Ramps.map { it to if (it == 0) stringResource(R.string.ramp_off) else stringResource(R.string.seconds_value, it) },
            alarm.rampSeconds, { onChange(alarm.copy(rampSeconds = it)) },
        )
        Spacer(Modifier.height(Space.s3))
    }
}

/** Five rising blocks, filled up to the chosen level (a flat stepped meter, not a slider). */
@Composable
private fun VolumeBlocks(value: Int, onSelect: (Int) -> Unit) {
    val c = Clookie.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s2), verticalAlignment = Alignment.Bottom) {
        Volumes.forEachIndexed { i, v ->
            val on = v <= value
            val shape = Clookie.shapes.small
            Column(
                Modifier
                    .weight(1f)
                    .height(36.dp + 14.dp * i)
                    .pressable(shape, { onSelect(v) }, shadow = false)
                    .background(if (on) c.ink else c.surface)
                    .border(Stroke.regular, c.outline, shape)
                    .padding(bottom = Space.s1),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Txt("$v", Clookie.type.label, color = if (on) c.background else c.ink, maxLines = 1)
            }
        }
    }
}

@Composable
fun VibrationSheet(pattern: VibrationPattern, onSelect: (VibrationPattern) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        SheetTitle(stringResource(R.string.vibration))
        ChoiceChips(VibrationPattern.entries.map { it to stringResource(it.nameRes) }, pattern, onSelect)
        Spacer(Modifier.height(Space.s3))
    }
}

@Composable
fun SnoozeSheet(alarm: Alarm, onChange: (Alarm) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        SheetTitle(stringResource(R.string.snooze))
        Eyebrow(stringResource(R.string.snooze_length))
        Spacer(Modifier.height(Space.s2))
        ChoiceChips(
            SnoozeLengths.map { it to stringResource(R.string.minutes_value, it) },
            alarm.snoozeMinutes, { onChange(alarm.copy(snoozeMinutes = it)) },
        )
        Spacer(Modifier.height(Space.s4))
        Eyebrow(stringResource(R.string.snooze_count))
        Spacer(Modifier.height(Space.s2))
        ChoiceChips(
            SnoozeLimits.map { it to if (it == 0) stringResource(R.string.snooze_off) else "×$it" },
            alarm.snoozeLimit, { onChange(alarm.copy(snoozeLimit = it)) },
        )
        Spacer(Modifier.height(Space.s3))
    }
}

/** Mission chain (up to 3) with each mission's difficulty settings, plus missions to add. */
@Composable
fun MissionSheet(chain: List<MissionSpec>, onChange: (List<MissionSpec>) -> Unit) {
    val c = Clookie.colors
    Column(Modifier.fillMaxWidth()) {
        SheetTitle(stringResource(R.string.missions))
        Column(verticalArrangement = Arrangement.spacedBy(Space.s3)) {
            chain.forEachIndexed { index, spec ->
                val (mission, resolved) = MissionRegistry.resolve(spec)
                val shape = Clookie.shapes.medium
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(c.background, shape)
                        .border(Stroke.regular, c.outline, shape)
                        .padding(Space.s4),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Txt("${index + 1}", Clookie.type.displaySmall, color = c.accent)
                        Spacer(Modifier.width(Space.s3))
                        ClookieIcon(mission.type.icon, null, size = 22.dp)
                        Spacer(Modifier.width(Space.s2))
                        Txt(stringResource(mission.type.nameRes), Clookie.type.heading, Modifier.weight(1f), maxLines = 1)
                        if (chain.size > 1) {
                            IconCircleButton(
                                ClookieIcons.Close, stringResource(R.string.mission_remove),
                                { onChange(chain.filterIndexed { i, _ -> i != index }) },
                                size = 40.dp,
                            )
                        }
                    }
                    mission.settings.forEach { setting ->
                        Spacer(Modifier.height(Space.s3))
                        Txt(stringResource(setting.label), Clookie.type.label, color = c.inkMuted)
                        Spacer(Modifier.height(Space.s2))
                        ChoiceChips(
                            setting.values.mapIndexed { i, v ->
                                v to (setting.valueLabels?.getOrNull(i)?.let { stringResource(it) } ?: "$v")
                            },
                            resolved.param(setting.key, setting.default),
                            { v ->
                                onChange(chain.mapIndexed { i, s -> if (i == index) resolved.copy(params = resolved.params + (setting.key to v)) else s })
                            },
                        )
                    }
                }
            }
        }
        val used = chain.map { it.type }.toSet()
        val addable = MissionRegistry.all.filter { it.type !in used }
        if (addable.isNotEmpty() && chain.size < Alarm.MaxMissions) {
            Spacer(Modifier.height(Space.s4))
            Eyebrow(stringResource(R.string.mission_add))
            Spacer(Modifier.height(Space.s2))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                addable.forEach { m ->
                    PillChip(
                        stringResource(m.type.nameRes), selected = false, icon = m.type.icon,
                        onClick = { onChange(chain + MissionRegistry.defaultSpec(m)) },
                    )
                }
            }
        }
        Spacer(Modifier.height(Space.s3))
    }
}

@Composable
fun rememberSoundName(id: String): String {
    val context = LocalContext.current
    val builtIn = (SoundRef.parse(id) as? SoundRef.BuiltIn)?.let { stringResource(it.sound.nameRes) }
    val name by produceState(builtIn ?: "", id) { if (builtIn == null) value = Sounds.displayName(context, id) }
    return builtIn ?: name
}
