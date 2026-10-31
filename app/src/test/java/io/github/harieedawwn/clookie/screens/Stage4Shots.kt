package io.github.harieedawwn.clookie.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.harieedawwn.clookie.design.ClookieTheme
import io.github.harieedawwn.clookie.design.Presets
import io.github.harieedawwn.clookie.mission.LocalMissionDemo
import io.github.harieedawwn.clookie.model.Alarm
import io.github.harieedawwn.clookie.model.MissionKeys
import io.github.harieedawwn.clookie.model.MissionSpec
import io.github.harieedawwn.clookie.model.MissionType
import io.github.harieedawwn.clookie.model.SleeperProfile
import io.github.harieedawwn.clookie.model.SleeperProfiles
import io.github.harieedawwn.clookie.ui.LocalNow
import io.github.harieedawwn.clookie.ui.screens.AlarmEditorScreen
import io.github.harieedawwn.clookie.ui.screens.EditorSheet
import io.github.harieedawwn.clookie.ui.screens.RingingScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime

/** Stage 4 review screenshots: every new mission on the ringing screen (Bơ sữa) -> design/screens/s4_*. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Stage4Shots {
    private val now = mutableStateOf(LocalDateTime.of(2026, 11, 2, 6, 31))

    private fun shot(name: String, content: @Composable () -> Unit) {
        captureRoboImage("../design/screens/$name.png") {
            ClookieTheme(Presets.BoSua, reducedMotion = true) {
                CompositionLocalProvider(LocalNow provides now, LocalMissionDemo provides true) {
                    Box(Modifier.fillMaxSize()) { content() }
                }
            }
        }
    }

    private fun alarm(spec: MissionSpec) = Alarm(id = 9, hour = 6, minute = 30, label = "Dậy đi học", missions = listOf(spec))

    @Test
    @Config(qualifiers = "vi-w412dp-h915dp-xhdpi")
    fun missionsOnTheRingingScreen() {
        val missions = listOf(
            MissionSpec(MissionType.Math, mapOf(MissionKeys.Level to 2, MissionKeys.Count to 3)),
            MissionSpec(MissionType.Shake),
            MissionSpec(MissionType.Steps),
            MissionSpec(MissionType.Typing, mapOf(MissionKeys.Count to 2)),
            MissionSpec(MissionType.Memory),
            MissionSpec(MissionType.Crumbs),
        )
        for (m in missions) {
            shot("s4_ringing_${m.type.name.lowercase()}_bo_sua") { RingingScreen(alarm(m), now.value.toLocalDate(), {}, {}, preview = false) }
        }
    }

    @Test
    @Config(qualifiers = "vi-w412dp-h915dp-xhdpi")
    fun profileSheet() {
        val deep = SleeperProfiles.apply(Alarm(id = 9, hour = 6, minute = 30, label = "Dậy đi học"), SleeperProfile.Deep)
        shot("s4_editor_profile_bo_sua") {
            AlarmEditorScreen(deep, isNew = false, onSave = {}, onClose = {}, onDelete = {}, onPreview = {}, initialSheet = EditorSheet.Profile)
        }
    }
}
