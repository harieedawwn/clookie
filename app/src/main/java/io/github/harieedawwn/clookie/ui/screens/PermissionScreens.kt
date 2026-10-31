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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.icons.ClookieIcon
import io.github.harieedawwn.clookie.design.icons.ClookieIcons
import io.github.harieedawwn.clookie.reliability.PermItem
import io.github.harieedawwn.clookie.reliability.PermRow
import io.github.harieedawwn.clookie.reliability.PermState
import io.github.harieedawwn.clookie.reliability.requiredSettled
import io.github.harieedawwn.clookie.ui.Eyebrow
import io.github.harieedawwn.clookie.ui.TopBar
import io.github.harieedawwn.clookie.ui.components.ButtonKind
import io.github.harieedawwn.clookie.ui.components.ClookieMark
import io.github.harieedawwn.clookie.ui.components.CookieButton
import io.github.harieedawwn.clookie.ui.components.IconCircleButton
import io.github.harieedawwn.clookie.ui.components.PermissionRow
import io.github.harieedawwn.clookie.ui.components.RowDivider
import io.github.harieedawwn.clookie.ui.components.RowGroup
import io.github.harieedawwn.clookie.ui.components.Txt
import io.github.harieedawwn.clookie.ui.screenBackground

/**
 * First-launch permission screen (also reachable from Độ tin cậy): required and optional rows,
 * "Cấp tất cả" asks the missing ones one after another, "Xong" once required rows are settled.
 */
@Composable
fun PermissionScreen(
    rows: List<PermRow>,
    onGrant: (PermItem) -> Unit,
    onSkip: (PermItem) -> Unit,
    onGrantAll: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val c = Clookie.colors
    val missing = rows.any { it.state != PermState.Granted }
    Box(modifier.fillMaxSize().screenBackground()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = Space.gutter)
                .padding(top = Space.s2, bottom = 168.dp),
            verticalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            if (onBack != null) {
                TopBar(
                    stringResource(R.string.permissions_title),
                    leading = { IconCircleButton(ClookieIcons.Back, stringResource(R.string.action_back), onBack) },
                )
            } else {
                Row(Modifier.padding(top = Space.s4), verticalAlignment = Alignment.CenterVertically) {
                    ClookieMark(size = 44.dp)
                    Spacer(Modifier.width(Space.s3))
                    Txt(stringResource(R.string.permissions_title), Clookie.type.displayTitle)
                }
                Spacer(Modifier.height(Space.s2))
            }
            Section(R.string.perm_required, rows.filter { it.item.required }, onGrant, onSkip)
            Spacer(Modifier.height(Space.s2))
            Section(R.string.perm_optional, rows.filter { !it.item.required }, onGrant, null)
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(c.background)
                .navigationBarsPadding()
                .padding(horizontal = Space.gutter, vertical = Space.s3),
            verticalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            if (missing) {
                CookieButton(stringResource(R.string.perm_grant_all), onGrantAll, Modifier.fillMaxWidth(), icon = ClookieIcons.Shield)
            }
            CookieButton(
                stringResource(R.string.action_done), onDone, Modifier.fillMaxWidth(),
                kind = if (missing) ButtonKind.Secondary else ButtonKind.Primary,
                icon = ClookieIcons.Check,
                enabled = rows.requiredSettled(),
            )
        }
    }
}

@Composable
private fun Section(title: Int, rows: List<PermRow>, onGrant: (PermItem) -> Unit, onSkip: ((PermItem) -> Unit)?) {
    if (rows.isEmpty()) return
    Eyebrow(stringResource(title))
    RowGroup {
        rows.forEachIndexed { i, row ->
            if (i > 0) RowDivider()
            PermissionRow(row, onGrant = { onGrant(row.item) }, onSkip = onSkip?.let { { it(row.item) } })
        }
    }
}

/** Độ tin cậy: status of everything an alarm needs, one-tap fixes, and the full permission list. */
@Composable
fun ReliabilityScreen(
    rows: List<PermRow>,
    onGrant: (PermItem) -> Unit,
    onAllPermissions: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Clookie.colors
    val required = rows.filter { it.item.required }
    val issues = required.count { it.state != PermState.Granted }
    Column(
        modifier
            .fillMaxSize()
            .screenBackground()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Space.gutter)
            .padding(top = Space.s2, bottom = Space.s8),
        verticalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        TopBar(
            stringResource(R.string.reliability),
            leading = { IconCircleButton(ClookieIcons.Back, stringResource(R.string.action_back), onBack) },
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = Space.s2)
                .background(if (issues == 0) c.accentSoft else c.accent, Clookie.shapes.large)
                .padding(Space.s5),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val fg = if (issues == 0) c.onAccentSoft else c.onAccent
            ClookieIcon(ClookieIcons.Shield, null, tint = fg, size = 40.dp)
            Spacer(Modifier.width(Space.s4))
            Txt(
                if (issues == 0) stringResource(R.string.reliability_ok) else stringResource(R.string.reliability_issues, issues),
                Clookie.type.title, color = fg,
            )
        }
        RowGroup {
            required.forEachIndexed { i, row ->
                if (i > 0) RowDivider()
                PermissionRow(row, onGrant = { onGrant(row.item) })
            }
        }
        Spacer(Modifier.height(Space.s2))
        CookieButton(stringResource(R.string.all_permissions), onAllPermissions, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary, icon = ClookieIcons.Settings)
    }
}
