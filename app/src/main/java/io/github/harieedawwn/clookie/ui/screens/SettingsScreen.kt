package io.github.harieedawwn.clookie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Preset
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.design.icons.ClookieIcons
import io.github.harieedawwn.clookie.ui.Eyebrow
import io.github.harieedawwn.clookie.ui.IconChip
import io.github.harieedawwn.clookie.ui.SettingsRow
import io.github.harieedawwn.clookie.ui.components.ButtonKind
import io.github.harieedawwn.clookie.ui.components.CookieButton
import io.github.harieedawwn.clookie.ui.components.Segmented
import io.github.harieedawwn.clookie.ui.components.Txt
import io.github.harieedawwn.clookie.ui.screenBackground
import io.github.harieedawwn.clookie.model.Alarm
import io.github.harieedawwn.clookie.ui.UpdateUi
import io.github.harieedawwn.clookie.ui.components.ClookieSheet
import io.github.harieedawwn.clookie.ui.info
import io.github.harieedawwn.clookie.ui.updateActionText

/** Settings: appearance, language, Độ tin cậy, default sound, updates. */
@Composable
fun SettingsScreen(
    preset: Preset,
    language: String,
    versionName: String,
    onLanguage: (String) -> Unit,
    onTheme: () -> Unit,
    onReliability: () -> Unit,
    update: UpdateUi,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 120.dp,
    reliabilityIssue: Boolean = false,
    defaultSound: String = Alarm.DefaultSound,
    onDefaultSound: (String) -> Unit = {},
) {
    val c = Clookie.colors
    var soundSheet by remember { mutableStateOf(false) }
    Box(modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .screenBackground()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = Space.gutter)
            .padding(top = Space.s6, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Txt(stringResource(R.string.nav_settings), Clookie.type.displayTitle)
        Spacer(Modifier.height(Space.s2))

        Eyebrow(stringResource(R.string.appearance))
        Group {
            SettingsRow(ClookieIcons.Cookie, stringResource(R.string.theme), stringResource(preset.nameRes), onTheme)
        }
        Eyebrow(stringResource(R.string.language))
        Segmented(
            options = listOf("vi" to "Tiếng Việt", "en" to "English"),
            selected = language,
            onSelect = onLanguage,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Space.s2))
        Eyebrow(stringResource(R.string.alarms))
        Group {
            SettingsRow(
                ClookieIcons.Shield, stringResource(R.string.reliability),
                stringResource(if (reliabilityIssue) R.string.perm_missing else R.string.reliability_ok), onReliability,
            )
            Divider()
            SettingsRow(ClookieIcons.Sound, stringResource(R.string.default_sound), rememberSoundName(defaultSound), { soundSheet = true })
            Divider()
            SettingsRow(ClookieIcons.Alarm, stringResource(R.string.time_format), stringResource(R.string.time_format_24), {})
            Divider()
            SettingsRow(ClookieIcons.Repeat, stringResource(R.string.week_starts), stringResource(R.string.monday), {})
        }

        Spacer(Modifier.height(Space.s2))
        Eyebrow(stringResource(R.string.updates))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(Clookie.shapes.medium)
                .background(c.surface)
                .border(Stroke.thin, c.outline, Clookie.shapes.medium)
                .padding(Space.s4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconChip(ClookieIcons.Download)
            Column(Modifier.weight(1f).padding(horizontal = Space.s3)) {
                Txt("Clookie", Clookie.type.bodyStrong)
                val status = when (update) {
                    UpdateUi.Latest -> stringResource(R.string.update_latest)
                    else -> update.info()?.let { stringResource(R.string.update_available, it.version) }
                }
                Txt(
                    stringResource(R.string.version_value, versionName) + (status?.let { " · $it" } ?: ""),
                    Clookie.type.label, color = c.inkMuted, maxLines = 2,
                )
            }
            CookieButton(
                updateActionText(update), onUpdate,
                kind = if (update.info() != null) ButtonKind.Primary else ButtonKind.Secondary,
                enabled = update !is UpdateUi.Downloading && update != UpdateUi.Checking,
            )
        }
    }
    ClookieSheet(soundSheet, onDismiss = { soundSheet = false }) {
        SoundSheet(defaultSound, onDefaultSound)
    }
    }
}

@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    val c = Clookie.colors
    val shape = Clookie.shapes.medium
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(Stroke.thin, c.outline, shape),
        content = content,
    )
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().padding(start = 64.dp).height(1.dp).background(Clookie.colors.outline.copy(alpha = 0.25f)))
}
