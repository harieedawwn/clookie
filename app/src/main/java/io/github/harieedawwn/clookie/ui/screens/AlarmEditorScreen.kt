package io.github.harieedawwn.clookie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.TimeSize
import io.github.harieedawwn.clookie.design.icons.ClookieIcon
import io.github.harieedawwn.clookie.design.icons.ClookieIcons
import io.github.harieedawwn.clookie.model.Alarm
import io.github.harieedawwn.clookie.model.SleeperProfiles
import io.github.harieedawwn.clookie.model.VibrationPattern
import io.github.harieedawwn.clookie.model.countdown
import io.github.harieedawwn.clookie.model.isSkippingNext
import io.github.harieedawwn.clookie.model.nextRing
import io.github.harieedawwn.clookie.model.withSkipNext
import io.github.harieedawwn.clookie.ui.Eyebrow
import io.github.harieedawwn.clookie.ui.LocalNow
import io.github.harieedawwn.clookie.ui.Tile
import io.github.harieedawwn.clookie.ui.TopBar
import io.github.harieedawwn.clookie.ui.components.BiteToggle
import io.github.harieedawwn.clookie.ui.components.ClookieSheet
import io.github.harieedawwn.clookie.ui.components.CookieButton
import io.github.harieedawwn.clookie.ui.components.CookieDial
import io.github.harieedawwn.clookie.ui.components.IconCircleButton
import io.github.harieedawwn.clookie.ui.components.PillChip
import io.github.harieedawwn.clookie.ui.components.TimeDisplay
import io.github.harieedawwn.clookie.ui.components.TimeKeypad
import io.github.harieedawwn.clookie.ui.components.Txt
import io.github.harieedawwn.clookie.ui.components.WeekdayDots
import io.github.harieedawwn.clookie.ui.components.countdownText
import io.github.harieedawwn.clookie.ui.components.icon
import io.github.harieedawwn.clookie.ui.components.nameRes
import io.github.harieedawwn.clookie.ui.screenBackground
import io.github.harieedawwn.clookie.ui.sharedAlarm
import java.time.ZoneId

/**
 * Alarm editor: tap the digits for the keypad, or turn the Cookie Dial. Every tile opens its
 * sheet. [onPreview] runs the "Thử" preview of the mission chain.
 */
@Composable
fun AlarmEditorScreen(
    initial: Alarm,
    isNew: Boolean,
    onSave: (Alarm) -> Unit,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onPreview: (Alarm) -> Unit,
    modifier: Modifier = Modifier,
    initialSheet: EditorSheet? = null,
) {
    var alarm by remember { mutableStateOf(initial) }
    var sheet by remember { mutableStateOf(initialSheet) }
    var lastSheet by remember { mutableStateOf(initialSheet ?: EditorSheet.Keypad) }
    fun open(s: EditorSheet) {
        lastSheet = s
        sheet = s
    }
    val c = Clookie.colors
    val now = LocalNow.current
    val zone = remember { ZoneId.systemDefault() }

    Box(modifier.fillMaxSize().sharedAlarm(initial.id).screenBackground()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = Space.gutter)
                .padding(top = Space.s2, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(
                title = stringResource(if (isNew) R.string.new_alarm else R.string.edit_alarm),
                leading = { IconCircleButton(ClookieIcons.Close, stringResource(R.string.action_close), onClose) },
                trailing = if (isNew) null else {
                    { IconCircleButton(ClookieIcons.Trash, stringResource(R.string.action_delete), onDelete) }
                },
            )
            Spacer(Modifier.height(Space.s3))
            TimeDisplay(
                alarm.hour, alarm.minute, TimeSize.Dial.dp,
                minuteColor = c.accent,
                onFieldClick = { open(EditorSheet.Keypad) },
            )
            RingsIn(alarm)
            Spacer(Modifier.height(Space.s4))
            CookieDial(alarm.hour, alarm.minute, onChange = { h, m -> alarm = alarm.copy(hour = h, minute = m, oneShotAt = null) })
            Spacer(Modifier.height(Space.s6))

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.s3)) {
                Eyebrow(stringResource(R.string.repeat))
                WeekdayDots(
                    alarm.days, Modifier.fillMaxWidth(), dotSize = 42.dp,
                    onToggle = { alarm = alarm.copy(days = alarm.days.toggle(it), skipDate = null) },
                )
                Spacer(Modifier.height(Space.s2))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s3)) {
                    Tile(
                        ClookieIcons.Mission, stringResource(R.string.missions),
                        stringResource(alarm.missionTypes.first().nameRes) +
                            if (alarm.missions.size > 1) " +${alarm.missions.size - 1}" else "",
                        Modifier.weight(1f),
                        onClick = { open(EditorSheet.Missions) },
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                alarm.missionTypes.forEach { ClookieIcon(it.icon, null, size = 18.dp) }
                            }
                        },
                    )
                    Tile(
                        ClookieIcons.Moon, stringResource(R.string.sleeper_profile),
                        SleeperProfiles.matching(alarm)?.let { stringResource(it.nameRes) } ?: stringResource(R.string.custom),
                        Modifier.weight(1f),
                        onClick = { open(EditorSheet.Profile) },
                    )
                }
                PillChip(
                    stringResource(R.string.action_try), selected = true, icon = ClookieIcons.Play,
                    onClick = { onPreview(alarm) }, modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s3)) {
                    Tile(
                        ClookieIcons.Label, stringResource(R.string.label), alarm.label.ifEmpty { "—" }, Modifier.weight(1f),
                        onClick = { open(EditorSheet.Label) },
                    )
                    Tile(
                        ClookieIcons.Sound, stringResource(R.string.sound), rememberSoundName(alarm.sound), Modifier.weight(1f),
                        onClick = { open(EditorSheet.Sound) },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s3)) {
                    Tile(
                        ClookieIcons.Ramp, stringResource(R.string.volume),
                        if (alarm.rampSeconds > 0) stringResource(R.string.volume_value_ramp, alarm.volume, alarm.rampSeconds)
                        else stringResource(R.string.volume_value, alarm.volume),
                        Modifier.weight(1f),
                        onClick = { open(EditorSheet.Volume) },
                    )
                    Tile(
                        ClookieIcons.Snooze, stringResource(R.string.snooze),
                        if (alarm.snoozeLimit == 0) stringResource(R.string.snooze_off)
                        else stringResource(R.string.snooze_value, alarm.snoozeMinutes, alarm.snoozeLimit),
                        Modifier.weight(1f),
                        onClick = { open(EditorSheet.Snooze) },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s3)) {
                    val vibrates = alarm.vibration != VibrationPattern.Off
                    Tile(
                        ClookieIcons.Vibrate, stringResource(R.string.vibration),
                        stringResource(alarm.vibration.nameRes),
                        Modifier.weight(1f),
                        onClick = { open(EditorSheet.Vibration) },
                        trailing = {
                            BiteToggle(vibrates, {
                                alarm = alarm.copy(vibration = if (it) VibrationPattern.Heartbeat else VibrationPattern.Off)
                            })
                        },
                    )
                    val repeating = !alarm.days.isOneTime
                    val skipping = alarm.isSkippingNext(now.value.atZone(zone))
                    Tile(
                        ClookieIcons.Skip, stringResource(R.string.skip_next),
                        if (!repeating) "—" else stringResource(if (skipping) R.string.on else R.string.off),
                        Modifier.weight(1f),
                        trailing = if (repeating) {
                            { BiteToggle(skipping, { alarm = alarm.withSkipNext(it, now.value.atZone(zone)) }) }
                        } else null,
                    )
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(c.background)
                .navigationBarsPadding()
                .padding(horizontal = Space.gutter, vertical = Space.s3),
        ) {
            CookieButton(
                stringResource(R.string.action_save), { onSave(alarm.copy(profile = SleeperProfiles.matching(alarm))) },
                Modifier.fillMaxWidth(), icon = ClookieIcons.Check,
            )
        }

        ClookieSheet(sheet != null, onDismiss = { sheet = null }) {
            when (lastSheet) {
                EditorSheet.Keypad -> {
                    TimeKeypad(alarm.hour, alarm.minute, onDone = { h, m ->
                        alarm = alarm.copy(hour = h, minute = m, oneShotAt = null)
                        sheet = null
                    })
                    Spacer(Modifier.height(Space.s3))
                }
                EditorSheet.Label -> LabelSheet(alarm.label) {
                    alarm = alarm.copy(label = it)
                    sheet = null
                }
                EditorSheet.Sound -> SoundSheet(alarm.sound) { alarm = alarm.copy(sound = it) }
                EditorSheet.Volume -> VolumeSheet(alarm) { alarm = it }
                EditorSheet.Vibration -> VibrationSheet(alarm.vibration) { alarm = alarm.copy(vibration = it) }
                EditorSheet.Snooze -> SnoozeSheet(alarm) { alarm = it }
                EditorSheet.Missions -> MissionSheet(alarm.missions) { alarm = alarm.copy(missions = it) }
                EditorSheet.Profile -> ProfileSheet(alarm) { alarm = it }
            }
        }
    }
}

@Composable
private fun RingsIn(alarm: Alarm) {
    val now = LocalNow.current
    val left by remember(alarm) {
        derivedStateOf {
            alarm.copy(enabled = true, snoozedUntil = null).nextRing(now.value)?.let { countdown(now.value, it) }
        }
    }
    left?.let {
        Txt(stringResource(R.string.rings_in, countdownText(it)), Clookie.type.label, color = Clookie.colors.inkMuted)
    }
}
