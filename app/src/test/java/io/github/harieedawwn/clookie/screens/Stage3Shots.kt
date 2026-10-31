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
import io.github.harieedawwn.clookie.design.Preset
import io.github.harieedawwn.clookie.design.Presets
import io.github.harieedawwn.clookie.mission.CodeSamples
import io.github.harieedawwn.clookie.mission.CookieCodeStudio
import io.github.harieedawwn.clookie.mission.KeyCode
import io.github.harieedawwn.clookie.mission.LocalCodeSamples
import io.github.harieedawwn.clookie.mission.QrCodesScreen
import io.github.harieedawwn.clookie.model.Alarm
import io.github.harieedawwn.clookie.model.CookieCode
import io.github.harieedawwn.clookie.model.MissionSpec
import io.github.harieedawwn.clookie.model.MissionType
import io.github.harieedawwn.clookie.model.ScanCode
import io.github.harieedawwn.clookie.scan.Decoded
import io.github.harieedawwn.clookie.ui.LocalNow
import io.github.harieedawwn.clookie.ui.screens.AlarmEditorScreen
import io.github.harieedawwn.clookie.ui.screens.EditorSheet
import io.github.harieedawwn.clookie.ui.screens.RingingScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime

/** Stage 3 review screenshots (Cookie Code, QR / barcode) -> design/screens/s3_*. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Stage3Shots {
    private val now = mutableStateOf(LocalDateTime.of(2026, 11, 2, 6, 31))
    private val cookie = listOf(CookieCode(1, "Phòng tắm", 0b010_011_100), CookieCode(2, "Bếp", 0b001_101_110))
    private val scan = listOf(
        ScanCode(3, "Dầu gội", "EAN_13", "8934588012112"),
        ScanCode(4, "Wi-Fi", "QR_CODE", "WIFI:S:Clookie;T:WPA;;"),
    )

    private fun shot(name: String, preset: Preset, samples: CodeSamples = CodeSamples(cookie, scan), content: @Composable () -> Unit) {
        captureRoboImage("../design/screens/$name.png") {
            ClookieTheme(preset, reducedMotion = true) {
                CompositionLocalProvider(LocalNow provides now, LocalCodeSamples provides samples) {
                    Box(Modifier.fillMaxSize()) { content() }
                }
            }
        }
    }

    private fun alarm(vararg missions: MissionSpec) = Alarm(id = 9, hour = 6, minute = 30, label = "Dậy đi học", missions = missions.toList())

    @Test
    @Config(qualifiers = "vi-w412dp-h915dp-xhdpi")
    fun ringingScanAndBackup() {
        val code = alarm(MissionSpec(MissionType.CookieCode, mapOf(KeyCode to 1)))
        for (p in listOf(Presets.BoSua, Presets.Dem)) {
            shot("s3_ringing_code_${p.id}", p) { RingingScreen(code, now.value.toLocalDate(), {}, {}, preview = false) }
        }
        shot("s3_ringing_qr_giay_o_li", Presets.GiayOLi) {
            RingingScreen(alarm(MissionSpec(MissionType.Qr)), now.value.toLocalDate(), {}, {}, preview = false)
        }
        // No code saved yet: the mission falls back to math instead of trapping the user.
        shot("s3_ringing_fallback_bo_sua", Presets.BoSua, CodeSamples(emptyList(), emptyList())) {
            RingingScreen(code, now.value.toLocalDate(), {}, {}, preview = false)
        }
    }

    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xhdpi")
    fun ringingOnSmallScreen() {
        shot("s3_ringing_code_360_bo_sua", Presets.BoSua) {
            RingingScreen(alarm(MissionSpec(MissionType.CookieCode)), now.value.toLocalDate(), {}, {}, preview = false)
        }
        shot("s3_ringing_fallback_360_bo_sua", Presets.BoSua, CodeSamples(emptyList(), emptyList())) {
            RingingScreen(alarm(MissionSpec(MissionType.CookieCode)), now.value.toLocalDate(), {}, {}, preview = false)
        }
    }

    @Test
    @Config(qualifiers = "vi-w412dp-h915dp-xhdpi")
    fun studioAndEditor() {
        for (p in listOf(Presets.BoSua, Presets.Dem)) {
            shot("s3_studio_guide_${p.id}", p) { CookieCodeStudio(cookie, {}, { _, _ -> }, {}, {}, startNew = true, pattern = 0b011_010_110) }
        }
        shot("s3_studio_tested_giay_o_li", Presets.GiayOLi) {
            CookieCodeStudio(cookie, {}, { _, _ -> }, {}, {}, startNew = true, pattern = 0b011_010_110, tested = true)
        }
        shot("s3_studio_list_bo_sua", Presets.BoSua) { CookieCodeStudio(cookie, {}, { _, _ -> }, {}, {}, exportedId = 1) }
        shot("s3_editor_missions_bo_sua", Presets.BoSua) {
            AlarmEditorScreen(
                alarm(MissionSpec(MissionType.CookieCode, mapOf(KeyCode to 1)), MissionSpec(MissionType.Qr)),
                isNew = false, onSave = {}, onClose = {}, onDelete = {}, onPreview = {}, initialSheet = EditorSheet.Missions,
            )
        }
        shot("s3_qr_found_bo_sua", Presets.BoSua) {
            QrCodesScreen(scan, {}, { _, _ -> }, {}, startScanning = true, sample = Decoded("EAN_13", "8934588012112"))
        }
        shot("s3_qr_list_dem", Presets.Dem) { QrCodesScreen(scan, {}, { _, _ -> }, {}) }
    }

    @Test
    @Config(qualifiers = "en-w412dp-h915dp-xhdpi")
    fun studioInEnglish() {
        shot("s3_studio_guide_en_bo_sua", Presets.BoSua) { CookieCodeStudio(cookie, {}, { _, _ -> }, {}, {}, startNew = true, pattern = 0b011_010_110) }
    }
}
