package io.github.harieedawwn.clookie.mission

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.design.rememberHaptics
import io.github.harieedawwn.clookie.model.MissionKeys
import io.github.harieedawwn.clookie.model.MissionSpec
import io.github.harieedawwn.clookie.model.MissionTuning
import io.github.harieedawwn.clookie.model.MissionType
import io.github.harieedawwn.clookie.model.TypingCheck
import io.github.harieedawwn.clookie.ui.components.Txt
import kotlin.random.Random

/**
 * Typing: retype a phrase exactly (the user's own, or the built-in Vietnamese set) on the
 * system keyboard; Telex and VNI work because the compare is NFC-normalized and trimmed.
 */
object TypingMission : WakeMission {
    private val t = MissionTuning.Typing
    override val type = MissionType.Typing
    override val settings = listOf(
        MissionSetting(MissionKeys.Count, R.string.setting_problems, t.phraseCounts, t.DefaultCount),
        MissionSetting(MissionKeys.Length, R.string.setting_length, t.lengths, t.DefaultLength, listOf(R.string.length_short, R.string.length_long)),
    )

    @Composable
    override fun Content(session: MissionSession, modifier: Modifier) {
        val c = Clookie.colors
        val haptics = rememberHaptics()
        val demo = LocalMissionDemo.current || LocalInspectionMode.current
        val count = session.spec.param(MissionKeys.Count, t.DefaultCount).coerceIn(1, 5)
        val long = session.spec.param(MissionKeys.Length, t.DefaultLength) == 2
        val builtIn = stringArrayResource(if (long) R.array.typing_long else R.array.typing_short)
        val own = session.spec.text.trim()
        val seed = rememberSaveable { Random.nextLong() }
        val phrases = remember(seed, own) {
            if (own.isNotEmpty()) List(count) { own }
            else List(count) { builtIn.toList().shuffled(Random(seed))[it % builtIn.size] }
        }
        var index by rememberSaveable { mutableIntStateOf(0) }
        val target = phrases[index.coerceIn(0, phrases.lastIndex)]
        var typed by remember(index) { mutableStateOf(TextFieldValue(if (demo) target.take(target.length / 2) else "")) }
        val focus = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        LaunchedEffect(index) {
            if (!demo) runCatching {
                focus.requestFocus()
                keyboard?.show()
            }
        }
        fun check(final: Boolean) {
            if (TypingCheck.matches(typed.text, target)) {
                haptics.confirm()
                if (index + 1 >= phrases.size) session.complete() else index++
            } else if (final) {
                haptics.reject()
            }
        }
        val offTrack = typed.text.isNotBlank() && !TypingCheck.onTrack(typed.text, target)
        // The keyboard covers the bottom of the screen: the field stays above it.
        Column(modifier.imePadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (phrases.size > 1) {
                Txt(stringResource(R.string.backup_progress, index + 1, phrases.size).uppercase(), Clookie.type.eyebrow, color = c.onRinging)
                Spacer(Modifier.height(Space.s2))
            }
            Txt(target, Clookie.type.title, Modifier.fillMaxWidth(), color = c.onRinging, align = TextAlign.Center, maxLines = 3)
            Spacer(Modifier.height(Space.s4))
            BasicTextField(
                value = typed,
                onValueChange = {
                    typed = it
                    session.interact()
                    check(final = false)
                },
                textStyle = Clookie.type.title.merge(color = c.onRinging),
                cursorBrush = SolidColor(c.onRinging),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { check(final = true) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .border(if (offTrack) Stroke.regular * 2 else Stroke.regular, c.onRinging, Clookie.shapes.medium)
                            .padding(horizontal = Space.s4, vertical = Space.s3),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (typed.text.isEmpty()) Txt(stringResource(R.string.typing_hint), Clookie.type.body, color = c.onRinging)
                        inner()
                    }
                },
            )
            Spacer(Modifier.height(Space.s2))
            // Always takes its line, so the field never jumps while typing.
            Txt(if (offTrack) stringResource(R.string.typing_off) else " ", Clookie.type.bodyStrong, color = c.onRinging, maxLines = 1)
        }
    }

    @Composable
    override fun EditorExtras(spec: MissionSpec, onChange: (MissionSpec) -> Unit) {
        val c = Clookie.colors
        Spacer(Modifier.height(Space.s3))
        Txt(stringResource(R.string.typing_own), Clookie.type.label, color = c.inkMuted)
        Spacer(Modifier.height(Space.s2))
        BasicTextField(
            value = spec.text,
            onValueChange = { onChange(spec.copy(text = it.take(t.MaxCustomLength))) },
            textStyle = Clookie.type.body.merge(color = c.ink),
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .border(Stroke.regular, c.outline, Clookie.shapes.small)
                        .padding(horizontal = Space.s3, vertical = Space.s3),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (spec.text.isEmpty()) Txt(stringResource(R.string.typing_own_hint), Clookie.type.body, color = c.inkMuted)
                    inner()
                }
            },
        )
    }
}
