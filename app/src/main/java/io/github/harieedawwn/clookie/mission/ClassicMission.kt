package io.github.harieedawwn.clookie.mission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.model.MissionType
import io.github.harieedawwn.clookie.ui.components.SlideToStop

/** Classic: slide the cookie across the track, [KEY_SLIDES] times in a row. */
object ClassicMission : WakeMission {
    const val KEY_SLIDES = "slides"

    override val type = MissionType.Classic
    override val quickDismiss = true
    override val settings = listOf(MissionSetting(KEY_SLIDES, R.string.setting_slides, listOf(1, 2, 3), 1))

    @Composable
    override fun Content(session: MissionSession, modifier: Modifier) {
        val total = session.spec.param(KEY_SLIDES, 1).coerceIn(1, 3)
        var done by rememberSaveable { mutableIntStateOf(0) }
        val label = stringResource(R.string.slide_to_stop) + if (total > 1) " · ${done + 1}/$total" else ""
        key(done) {
            SlideToStop(
                label,
                onDone = {
                    if (done + 1 >= total) session.complete() else done++
                },
                modifier = modifier,
                onDrag = session::interact,
            )
        }
    }
}
