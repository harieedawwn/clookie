package io.github.harieedawwn.clookie.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.icons.ClookieIcon
import io.github.harieedawwn.clookie.design.ClockStyle
import io.github.harieedawwn.clookie.model.Alarm
import io.github.harieedawwn.clookie.ui.components.ClookieBottomNav
import io.github.harieedawwn.clookie.ui.components.NavDest
import io.github.harieedawwn.clookie.ui.components.PillChip
import io.github.harieedawwn.clookie.ui.components.Txt
import io.github.harieedawwn.clookie.ui.screenBackground

/** Tab screens with the floating bottom nav on top. */
@Composable
fun MainScaffold(tab: NavDest, onTab: (NavDest) -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize()) {
        content()
        ClookieBottomNav(tab, onTab, Modifier.align(Alignment.BottomCenter))
    }
}

/** Home tab with nav, used by the app, the theme picker preview and screenshots. */
@Composable
fun HomeChrome(
    alarms: List<Alarm>,
    reliabilityIssue: Boolean,
    onToggle: (Alarm, Boolean) -> Unit = { _, _ -> },
    onOpen: (Alarm) -> Unit = {},
    onAdd: () -> Unit = {},
    onNap: (Int) -> Unit = {},
    onReliability: () -> Unit = {},
    onTab: (NavDest) -> Unit = {},
    banner: (@Composable () -> Unit)? = null,
    clockStyle: ClockStyle = ClockStyle.Digital,
) {
    MainScaffold(NavDest.Alarms, onTab) {
        HomeScreen(alarms, onToggle, onOpen, onAdd, onNap, onReliability, reliabilityIssue, banner = banner, clockStyle = clockStyle)
    }
}
