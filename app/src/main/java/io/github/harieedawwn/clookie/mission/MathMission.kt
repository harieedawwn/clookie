package io.github.harieedawwn.clookie.mission

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.model.MissionKeys
import io.github.harieedawwn.clookie.model.MissionTuning
import io.github.harieedawwn.clookie.model.MissionType

/** Level chip labels shared by missions with three difficulties. */
internal val LevelLabels = listOf(R.string.level_easy, R.string.level_medium, R.string.level_hard)

/** Math: [MissionKeys.Count] problems of a level, typed on the ringing number pad. */
object MathMission : WakeMission {
    private val t = MissionTuning.Math
    override val type = MissionType.Math
    override val settings = listOf(
        MissionSetting(MissionKeys.Level, R.string.setting_level, t.levels, t.DefaultLevel, LevelLabels),
        MissionSetting(MissionKeys.Count, R.string.setting_problems, t.counts, t.DefaultCount),
    )

    @Composable
    override fun Content(session: MissionSession, modifier: Modifier) {
        val level = session.spec.param(MissionKeys.Level, t.DefaultLevel).coerceIn(1, 3)
        val count = session.spec.param(MissionKeys.Count, t.DefaultCount).coerceIn(1, 20)
        MathPad(session, level, count, null, modifier)
    }
}
