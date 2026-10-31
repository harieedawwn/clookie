package io.github.harieedawwn.clookie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.design.ClockStyle
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.design.TimeSize
import io.github.harieedawwn.clookie.design.icons.ClookieIcon
import io.github.harieedawwn.clookie.design.icons.ClookieIcons
import io.github.harieedawwn.clookie.model.Alarm
import io.github.harieedawwn.clookie.model.countdown
import io.github.harieedawwn.clookie.model.nextUp
import io.github.harieedawwn.clookie.ui.Eyebrow
import io.github.harieedawwn.clookie.ui.LocalNow
import io.github.harieedawwn.clookie.ui.components.AlarmSlab
import io.github.harieedawwn.clookie.ui.components.BiteAddButton
import io.github.harieedawwn.clookie.ui.components.ClookieLockup
import io.github.harieedawwn.clookie.ui.components.CookieButton
import io.github.harieedawwn.clookie.ui.components.CookieClock
import io.github.harieedawwn.clookie.ui.components.NowNumerals
import io.github.harieedawwn.clookie.ui.components.WordClockText
import io.github.harieedawwn.clookie.ui.components.IconCircleButton
import io.github.harieedawwn.clookie.ui.components.PillChip
import io.github.harieedawwn.clookie.ui.components.TimeDisplay
import io.github.harieedawwn.clookie.ui.components.Txt
import io.github.harieedawwn.clookie.ui.components.countdownText
import io.github.harieedawwn.clookie.ui.components.icon
import io.github.harieedawwn.clookie.ui.components.nameRes
import io.github.harieedawwn.clookie.ui.components.pillShape
import io.github.harieedawwn.clookie.ui.screenBackground
import io.github.harieedawwn.clookie.ui.sharedAlarm

val QuickNaps = listOf(10, 20, 30, 60)

/** Home: the next alarm in huge numerals with a live countdown, quick naps, alarm slabs. */
@Composable
fun HomeScreen(
    alarms: List<Alarm>,
    onToggle: (Alarm, Boolean) -> Unit,
    onOpen: (Alarm) -> Unit,
    onAdd: () -> Unit,
    onNap: (Int) -> Unit,
    onReliability: () -> Unit,
    reliabilityIssue: Boolean,
    modifier: Modifier = Modifier,
    bottomPadding: androidx.compose.ui.unit.Dp = 120.dp,
    banner: (@Composable () -> Unit)? = null,
    clockStyle: ClockStyle = ClockStyle.Digital,
) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val d = Clookie.style.density.scale
    LazyColumn(
        modifier.fillMaxSize().screenBackground(),
        contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, top = top + Space.s3, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(Space.s4 * d),
    ) {
        item(key = "top") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ClookieLockup()
                Spacer(Modifier.weight(1f))
                Box {
                    IconCircleButton(ClookieIcons.Shield, stringResource(R.string.reliability), onReliability)
                    if (reliabilityIssue) {
                        Box(
                            Modifier.align(Alignment.TopEnd).offset(2.dp, (-2).dp).size(14.dp)
                                .background(Clookie.colors.background, CircleShape).padding(2.dp)
                                .background(Clookie.colors.accent, CircleShape),
                        )
                    }
                }
            }
        }
        if (banner != null) item(key = "banner") { banner() }
        item(key = "hero") {
            when (clockStyle) {
                ClockStyle.Digital -> NextAlarmHero(alarms, TimeSize.Hero.dp * d)
                ClockStyle.Cookie -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CookieClock(Modifier.padding(top = Space.s4), size = 232.dp * d)
                    NextAlarmHero(alarms, TimeSize.Compact.dp)
                }
                ClockStyle.Words -> Column(Modifier.padding(top = Space.s4)) {
                    NowEyebrow()
                    Spacer(Modifier.height(Space.s2))
                    WordClockText()
                    NextAlarmHero(alarms, TimeSize.Compact.dp)
                }
                ClockStyle.Minimal -> Column(Modifier.padding(top = Space.s8)) {
                    NowEyebrow()
                    NowNumerals(TimeSize.Hero.dp * 0.78f * d, Modifier.padding(vertical = Space.s1))
                    MinimalNext(alarms)
                }
            }
        }
        item(key = "naps") {
            Column {
                Eyebrow(stringResource(R.string.quick_nap))
                Spacer(Modifier.height(Space.s2))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                    QuickNaps.forEach { m ->
                        PillChip("+$m", selected = false, onClick = { onNap(m) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        item(key = "header") {
            Row(Modifier.padding(top = Space.s4), verticalAlignment = Alignment.CenterVertically) {
                Txt(stringResource(R.string.alarms), Clookie.type.displayTitle)
                Spacer(Modifier.width(Space.s2))
                Txt(alarms.count { it.enabled }.toString() + "/" + alarms.size, Clookie.type.label, color = Clookie.colors.inkMuted)
                Spacer(Modifier.weight(1f))
                BiteAddButton(stringResource(R.string.add_alarm), onAdd)
            }
        }
        if (alarms.isEmpty()) item(key = "empty") { EmptyAlarms(onAdd) }
        items(alarms, key = { it.id }) { alarm ->
            AlarmSlab(
                alarm,
                onToggle = { onToggle(alarm, it) },
                onClick = { onOpen(alarm) },
                modifier = Modifier.sharedAlarm(alarm.id),
            )
        }
    }
}

@Composable
private fun EmptyAlarms(onAdd: () -> Unit) {
    val c = Clookie.colors
    val shape = Clookie.shapes.large
    Column(
        Modifier
            .fillMaxWidth()
            .border(Stroke.regular, c.outline, shape)
            .padding(vertical = Space.s8, horizontal = Space.s5),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ClookieIcon(ClookieIcons.Cookie, null, size = 56.dp, tint = c.inkMuted)
        Spacer(Modifier.height(Space.s3))
        Txt(stringResource(R.string.no_alarms), Clookie.type.heading, color = c.inkMuted)
        Spacer(Modifier.height(Space.s4))
        CookieButton(stringResource(R.string.add_alarm), onAdd, icon = ClookieIcons.Plus)
    }
}

@Composable
private fun NowEyebrow() {
    val now = LocalNow.current
    val day by remember { derivedStateOf { now.value.toLocalDate() } }
    val weekdays = stringArrayResource(R.array.weekday_full)
    val months = stringArrayResource(R.array.month_names)
    Eyebrow(stringResource(R.string.now) + " · " + weekdays[day.dayOfWeek.value - 1] + ", " + day.dayOfMonth + " " + months[day.monthValue - 1])
}

/** Minimal face: one quiet line for the next alarm. */
@Composable
private fun MinimalNext(alarms: List<Alarm>) {
    val now = LocalNow.current
    val list by rememberUpdatedState(alarms)
    val next by remember { derivedStateOf { list.nextUp(now.value) } }
    val up = next
    if (up == null) {
        Txt(stringResource(R.string.no_alarm_on), Clookie.type.bodyStrong, color = Clookie.colors.inkMuted)
        return
    }
    val left by remember(up.second) { derivedStateOf { countdown(now.value, up.second) } }
    Row(verticalAlignment = Alignment.CenterVertically) {
        ClookieIcon(ClookieIcons.Bell, null, tint = Clookie.colors.accent, size = 18.dp)
        Spacer(Modifier.width(Space.s2))
        Txt("%02d:%02d · ".format(up.second.hour, up.second.minute) + countdownText(left), Clookie.type.bodyStrong, color = Clookie.colors.inkMuted, maxLines = 1)
    }
}

@Composable
private fun NextAlarmHero(alarms: List<Alarm>, numerals: androidx.compose.ui.unit.Dp) {
    val now = LocalNow.current
    val list by rememberUpdatedState(alarms)
    val next by remember { derivedStateOf { list.nextUp(now.value) } }
    val c = Clookie.colors
    val up = next
    Column(Modifier.fillMaxWidth().padding(top = Space.s4)) {
        if (up == null) {
            Eyebrow(stringResource(R.string.next_alarm))
            Spacer(Modifier.height(Space.s2))
            Txt(stringResource(R.string.no_alarm_on), Clookie.type.displayTitle, color = c.inkMuted)
            return
        }
        val (alarm, at) = up
        val weekdays = stringArrayResource(R.array.weekday_full)
        val daysAhead = remember(at, now.value.toLocalDate()) { java.time.temporal.ChronoUnit.DAYS.between(now.value.toLocalDate(), at.toLocalDate()) }
        val dayText = when (daysAhead) {
            0L -> stringResource(R.string.today)
            1L -> stringResource(R.string.tomorrow)
            else -> weekdays[at.dayOfWeek.value - 1]
        }
        Eyebrow(stringResource(R.string.next_alarm) + " · " + dayText)
        TimeDisplay(at.hour, at.minute, numerals, Modifier.fillMaxWidth().padding(vertical = Space.s1))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s3)) {
            val left by remember(at) { derivedStateOf { countdown(now.value, at) } }
            Row(
                Modifier.background(c.accent, pillShape()).padding(horizontal = Space.s4, vertical = Space.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ClookieIcon(ClookieIcons.Bell, null, tint = c.onAccent, size = 18.dp)
                Spacer(Modifier.width(Space.s2))
                Txt(countdownText(left), Clookie.type.bodyStrong, color = c.onAccent, maxLines = 1)
            }
            if (alarm.label.isNotEmpty()) {
                Txt(alarm.label, Clookie.type.bodyStrong, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
            }
            alarm.missionTypes.forEach { ClookieIcon(it.icon, stringResource(it.nameRes), size = 20.dp) }
        }
    }
}
