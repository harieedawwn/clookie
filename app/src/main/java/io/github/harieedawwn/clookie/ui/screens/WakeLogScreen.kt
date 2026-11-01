package io.github.harieedawwn.clookie.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.design.hardShadow
import io.github.harieedawwn.clookie.design.icons.ClookieIcons
import io.github.harieedawwn.clookie.model.RingOutcome
import io.github.harieedawwn.clookie.model.WakeEntry
import io.github.harieedawwn.clookie.model.WakeStats
import io.github.harieedawwn.clookie.ui.Eyebrow
import io.github.harieedawwn.clookie.ui.TopBar
import io.github.harieedawwn.clookie.ui.components.IconCircleButton
import io.github.harieedawwn.clookie.ui.components.Txt
import io.github.harieedawwn.clookie.ui.screenBackground
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Duration as "2 phút 10 giây" / "40 giây". */
@Composable
fun durationText(ms: Long): String {
    val s = ms / 1000
    return if (s >= 60) stringResource(R.string.duration_ms, s / 60, s % 60) else stringResource(R.string.duration_s, s)
}

/** Wake log: on-time streak, a flat 14-day bar chart (minutes to stop), recent wakes. */
@Composable
fun WakeLogScreen(
    entries: List<WakeEntry>,
    today: LocalDate,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val c = Clookie.colors
    val days = remember(entries, today) { WakeStats.days(entries, today, zone) }
    val streak = remember(entries) { WakeStats.streak(entries, zone) }
    LazyColumn(
        modifier.fillMaxSize().screenBackground().statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = Space.s8),
        verticalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        item(key = "top") {
            TopBar(stringResource(R.string.wake_log), leading = { IconCircleButton(ClookieIcons.Back, stringResource(R.string.action_back), onBack) })
        }
        item(key = "streak") {
            val shape = Clookie.shapes.large
            Row(
                Modifier.fillMaxWidth().hardShadow(shape).background(c.accent, shape).border(Stroke.regular, c.outline, shape)
                    .padding(horizontal = Space.s5, vertical = Space.s4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt(streak.toString(), Clookie.type.displayTitle.copy(fontSize = 64.sp, lineHeight = 76.sp), color = c.onAccent)
                Spacer(Modifier.width(Space.s4))
                Column {
                    Txt(stringResource(R.string.days_unit), Clookie.type.title, color = c.onAccent)
                    Txt(stringResource(R.string.streak), Clookie.type.label, color = c.onAccent)
                }
            }
        }
        item(key = "chart") {
            Column(Modifier.padding(top = Space.s3)) {
                Eyebrow(stringResource(R.string.last_14))
                Spacer(Modifier.height(Space.s3))
                WakeChart(days)
            }
        }
        item(key = "recent") { Eyebrow(stringResource(R.string.recent_wakes), Modifier.padding(top = Space.s3)) }
        if (entries.isEmpty()) {
            item(key = "empty") { Txt(stringResource(R.string.no_wakes), Clookie.type.bodyStrong, color = c.inkMuted) }
        }
        items(entries.take(20), key = { it.scheduledAt.toString() + it.endedAt }) { WakeRow(it, zone) }
    }
}

/** 14 flat bars: height = minutes to stop (capped at 15), accent when on time; a dot for days without an alarm. */
@Composable
private fun WakeChart(days: List<io.github.harieedawwn.clookie.model.WakeDay>) {
    val c = Clookie.colors
    val labels = stringArrayResource(R.array.weekday_short)
    val description = stringResource(R.string.last_14)
    Column {
        Canvas(Modifier.fillMaxWidth().height(132.dp).semantics { contentDescription = description }) {
            val n = days.size
            val gap = 6.dp.toPx()
            val bw = (size.width - gap * (n - 1)) / n
            val base = size.height
            val maxMin = 15f
            drawLine(c.outline, Offset(0f, base - 1.dp.toPx()), Offset(size.width, base - 1.dp.toPx()), Stroke.regular.toPx())
            days.forEachIndexed { i, d ->
                val x = i * (bw + gap)
                val e = d.entry
                if (e == null) {
                    drawCircle(c.inkMuted, 2.5.dp.toPx(), Offset(x + bw / 2, base - 10.dp.toPx()))
                    return@forEachIndexed
                }
                val minutes = if (e.outcome == RingOutcome.Missed) maxMin else (e.timeToDismissMs / 60_000f).coerceIn(0.4f, maxMin)
                val h = (size.height - 8.dp.toPx()) * minutes / maxMin
                val color = when {
                    e.onTime -> c.accent
                    e.outcome == RingOutcome.Missed -> c.inkMuted
                    else -> c.ink
                }
                drawRoundRect(color, Offset(x, base - h), Size(bw, h), CornerRadius(minOf(bw / 2, 6.dp.toPx()), minOf(bw / 2, 6.dp.toPx())))
            }
        }
        Spacer(Modifier.height(Space.s2))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEach { d ->
                Txt(
                    labels[d.date.dayOfWeek.value - 1].take(2), Clookie.type.caption, Modifier.weight(1f),
                    color = Clookie.colors.inkMuted, maxLines = 1, align = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun WakeRow(e: WakeEntry, zone: ZoneId) {
    val c = Clookie.colors
    val shape = Clookie.shapes.medium
    val at = Instant.ofEpochMilli(e.endedAt).atZone(zone)
    val day = Instant.ofEpochMilli(e.scheduledAt).atZone(zone)
    val weekdays = stringArrayResource(R.array.weekday_short)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).background(c.surface, shape).border(Stroke.thin, c.outline, shape)
            .padding(horizontal = Space.s4, vertical = Space.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.widthIn(min = 56.dp).padding(end = Space.s2)) {
            Txt(weekdays[day.dayOfWeek.value - 1], Clookie.type.label, color = c.inkMuted, maxLines = 1)
            Txt("%d/%d".format(day.dayOfMonth, day.monthValue), Clookie.type.bodyStrong, maxLines = 1)
        }
        Column(Modifier.weight(1f)) {
            val time = if (e.outcome == RingOutcome.Missed) stringResource(R.string.wake_missed) else "%02d:%02d".format(at.hour, at.minute)
            Txt(time + (if (e.label.isNotEmpty()) " · " + e.label else ""), Clookie.type.bodyStrong, maxLines = 1)
            val parts = buildList {
                if (e.outcome != RingOutcome.Missed) add(stringResource(R.string.time_to_dismiss, durationText(e.timeToDismissMs)))
                if (e.snoozes > 0) add(stringResource(R.string.snoozes_n, e.snoozes))
            }
            if (parts.isNotEmpty()) Txt(parts.joinToString(" · "), Clookie.type.label, color = c.inkMuted, maxLines = 2)
        }
        val tag = when {
            e.onTime -> stringResource(R.string.on_time)
            e.outcome == RingOutcome.Missed -> stringResource(R.string.wake_missed)
            else -> stringResource(R.string.wake_late)
        }
        Txt(
            tag, Clookie.type.label, maxLines = 1,
            color = if (e.onTime) c.onAccent else c.onAccentSoft,
            modifier = Modifier.background(if (e.onTime) c.accent else c.accentSoft, Clookie.shapes.small).padding(horizontal = Space.s2, vertical = 2.dp),
        )
    }
}
