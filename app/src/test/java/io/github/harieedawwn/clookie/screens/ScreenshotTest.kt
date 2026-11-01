package io.github.harieedawwn.clookie.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.harieedawwn.clookie.design.ClookieTheme
import io.github.harieedawwn.clookie.design.Preset
import io.github.harieedawwn.clookie.design.Presets
import io.github.harieedawwn.clookie.model.Alarm
import io.github.harieedawwn.clookie.model.MissionSpec
import io.github.harieedawwn.clookie.model.MissionType
import io.github.harieedawwn.clookie.model.SampleAlarms
import io.github.harieedawwn.clookie.reliability.PermItem
import io.github.harieedawwn.clookie.reliability.permRows
import io.github.harieedawwn.clookie.ui.LocalNow
import io.github.harieedawwn.clookie.ui.UpdateUi
import io.github.harieedawwn.clookie.ui.components.NavDest
import io.github.harieedawwn.clookie.ui.screens.AlarmEditorScreen
import io.github.harieedawwn.clookie.ui.screens.EditorSheet
import io.github.harieedawwn.clookie.ui.screens.HomeChrome
import io.github.harieedawwn.clookie.ui.screens.MainScaffold
import io.github.harieedawwn.clookie.ui.screens.PermissionScreen
import io.github.harieedawwn.clookie.ui.screens.ReliabilityScreen
import io.github.harieedawwn.clookie.ui.screens.RingingScreen
import io.github.harieedawwn.clookie.ui.screens.SettingsScreen
import io.github.harieedawwn.clookie.ui.screens.StudioScreen
import io.github.harieedawwn.clookie.design.ClockStyle
import io.github.harieedawwn.clookie.design.Look
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime

/**
 * Design review screenshots -> design/screens/. Run: gradlew :app:recordRoborazziDebug.
 * Fixed clock: Monday 2026-11-02 23:18, so the next alarm is tomorrow 06:30.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {
    @get:org.junit.Rule val fontScale = FontScaleRule()

    private val now = mutableStateOf(LocalDateTime.of(2026, 11, 2, 23, 18))
    private val reviewPresets = listOf(Presets.BoSua, Presets.Socola, Presets.GiayOLi)

    /** Stage 2 checks run in a light and a dark preset. */
    private val stagePresets = listOf(Presets.BoSua, Presets.Dem)

    private fun shot(name: String, preset: Preset, fontScale: Float? = null, content: @Composable () -> Unit) {
        captureRoboImage("../design/screens/$name.png") {
            ClookieTheme(preset, reducedMotion = true) {
                val d = LocalDensity.current
                CompositionLocalProvider(
                    LocalNow provides now,
                    LocalDensity provides Density(d.density, fontScale ?: d.fontScale),
                ) { Box(Modifier.fillMaxSize()) { content() } }
            }
        }
    }

    @Composable
    private fun Home(alarms: List<Alarm> = SampleAlarms.list) = HomeChrome(alarms, reliabilityIssue = true)

    @Composable
    private fun Editor(sheet: EditorSheet? = null, alarm: Alarm = SampleAlarms.list[0]) = AlarmEditorScreen(
        alarm, isNew = false, onSave = {}, onClose = {}, onDelete = {}, onPreview = {}, initialSheet = sheet,
    )

    @Composable
    private fun Settings(preset: Preset) = MainScaffold(NavDest.Settings, {}) {
        SettingsScreen(preset, "vi", "0.2.0", {}, {}, {}, UpdateUi.Idle, {})
    }

    private val ringAlarm = Alarm(
        id = 9, hour = 6, minute = 30, label = "Đi làm",
        missions = listOf(MissionSpec(MissionType.Classic, mapOf("slides" to 2))), snoozeLimit = 3,
    )

    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xhdpi")
    fun fiveScreensInReviewPresets() {
        for (p in reviewPresets) {
            shot("home_${p.id}", p) { Home() }
            shot("editor_${p.id}", p) { Editor() }
            shot("ringing_${p.id}", p) { RingingScreen(SampleAlarms.list[1], now.value.toLocalDate().plusDays(1), {}, {}) }
            shot("themes_${p.id}", p) {
                StudioScreen(Look(p.id), ClockStyle.Digital, emptyList(), false, SampleAlarms.list, { _, _ -> }, { _, _ -> }, {}, {}, {})
            }
            shot("settings_${p.id}", p) { Settings(p) }
        }
        shot("editor_keypad_bo_sua", Presets.BoSua) { Editor(EditorSheet.Keypad) }
    }

    @Test
    @Config(qualifiers = "vi-w412dp-h915dp-xhdpi")
    fun homeInEveryPreset() {
        for (p in Presets.all) shot("home_412_${p.id}", p) { Home() }
    }

    @Test
    @Config(qualifiers = "vi-w412dp-h915dp-xhdpi")
    fun homeAtFontScale130() {
        shot("home_412_fontscale_130", Presets.BoSua, fontScale = 1.3f) { Home() }
    }

    @Test
    @Config(qualifiers = "en-w360dp-h640dp-xhdpi")
    fun homeInEnglish() {
        shot("home_en_bo_sua", Presets.BoSua) { Home() }
    }

    // --- Stage 2 -------------------------------------------------------------------------

    @Test
    @Config(qualifiers = "vi-w412dp-h915dp-xhdpi")
    fun stage2AlarmListEmptyAndOne() {
        for (p in stagePresets) {
            shot("s2_list_empty_${p.id}", p) { Home(emptyList()) }
            shot("s2_list_one_${p.id}", p) { Home(listOf(SampleAlarms.list[1])) }
        }
    }

    /** Tall canvas so all eight slabs are visible at once. */
    @Test
    @Config(qualifiers = "vi-w412dp-h2000dp-xhdpi")
    fun stage2AlarmListEight() {
        for (p in stagePresets) shot("s2_list_eight_${p.id}", p) { Home(SampleAlarms.eight) }
    }

    @Test
    @Config(qualifiers = "vi-w412dp-h915dp-xhdpi")
    fun stage2EditorRingingPermissions() {
        val missing = permRows(PermItem.entries, { false }, emptySet())
        val granted = permRows(PermItem.entries, { true }, emptySet())
        for (p in stagePresets) {
            shot("s2_editor_${p.id}", p) { Editor(alarm = SampleAlarms.list[1]) }
            shot("s2_editor_missions_${p.id}", p) { Editor(EditorSheet.Missions, ringAlarm) }
            shot("s2_ringing_${p.id}", p) {
                RingingScreen(ringAlarm, now.value.toLocalDate(), {}, {}, preview = false)
            }
            shot("s2_permissions_missing_${p.id}", p) { PermissionScreen(missing, {}, {}, {}, {}) }
            shot("s2_permissions_granted_${p.id}", p) { PermissionScreen(granted, {}, {}, {}, {}) }
        }
        shot("s2_reliability_bo_sua", Presets.BoSua) { ReliabilityScreen(missing, {}, {}, {}) }
        shot("s2_editor_volume_bo_sua", Presets.BoSua) { Editor(EditorSheet.Volume, SampleAlarms.list[1]) }
    }
}
