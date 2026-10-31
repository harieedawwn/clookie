package io.github.harieedawwn.clookie.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.design.icons.ClookieIcon
import io.github.harieedawwn.clookie.design.icons.ClookieIcons
import io.github.harieedawwn.clookie.reliability.PermItem
import io.github.harieedawwn.clookie.reliability.PermRow
import io.github.harieedawwn.clookie.reliability.PermState
import io.github.harieedawwn.clookie.ui.IconChip

/**
 * Permission row: icon chip, name and status. Granted rows show a check badge; others show
 * the status, "Bỏ qua" (required rows, if [onSkip]) and the one-tap "Cấp" on a second line.
 */
@Composable
fun PermissionRow(row: PermRow, onGrant: () -> Unit, modifier: Modifier = Modifier, onSkip: (() -> Unit)? = null) {
    val c = Clookie.colors
    Column(modifier.fillMaxWidth().padding(horizontal = Space.s4, vertical = Space.s3)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            IconChip(row.item.icon)
            Spacer(Modifier.width(Space.s3))
            Txt(stringResource(row.item.nameRes), Clookie.type.bodyStrong, Modifier.weight(1f), maxLines = 2)
            if (row.state == PermState.Granted) {
                Spacer(Modifier.width(Space.s2))
                StatusBadge(row)
            }
        }
        if (row.state != PermState.Granted) {
            Row(
                Modifier.fillMaxWidth().padding(top = Space.s2),
                horizontalArrangement = Arrangement.spacedBy(Space.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusBadge(row)
                Spacer(Modifier.weight(1f))
                if (onSkip != null && row.item.required && row.state == PermState.Missing) {
                    PillChip(stringResource(R.string.perm_skip), selected = false, onClick = onSkip)
                }
                PillChip(stringResource(R.string.perm_grant), selected = true, onClick = onGrant)
            }
        }
    }
}

@Composable
private fun StatusBadge(row: PermRow) {
    val c = Clookie.colors
    val shape = pillShape()
    val (text, bg, fg) = when (row.state) {
        PermState.Granted -> Triple(
            stringResource(if (row.item == PermItem.Autostart) R.string.perm_opened else R.string.perm_granted),
            c.accentSoft, c.onAccentSoft,
        )
        PermState.Missing -> Triple(stringResource(R.string.perm_missing), c.accent, c.onAccent)
        PermState.Skipped -> Triple(stringResource(R.string.perm_skipped), c.surface, c.inkMuted)
    }
    Row(
        Modifier
            .background(bg, shape)
            .then(if (row.state == PermState.Skipped) Modifier.border(Stroke.thin, c.outline, shape) else Modifier)
            .padding(horizontal = Space.s3, vertical = Space.s1 + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (row.state == PermState.Granted) {
            ClookieIcon(ClookieIcons.Check, null, tint = fg, size = 16.dp)
            Spacer(Modifier.width(Space.s1))
        }
        Txt(text, Clookie.type.label, color = fg, maxLines = 1)
    }
}

/** Grouped card for rows (same look as the Settings groups). */
@Composable
fun RowGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = Clookie.colors
    val shape = Clookie.shapes.medium
    Column(
        modifier
            .fillMaxWidth()
            .background(c.surface, shape)
            .border(Stroke.thin, c.outline, shape),
    ) { content() }
}

@Composable
fun RowDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 64.dp).heightIn(min = 1.dp, max = 1.dp).background(Clookie.colors.outline.copy(alpha = 0.25f)))
}
