package io.github.harieedawwn.clookie.data

import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.harieedawwn.clookie.data.db.CookieCodeEntity
import io.github.harieedawwn.clookie.data.db.ScanCodeEntity
import io.github.harieedawwn.clookie.design.ClockFont
import io.github.harieedawwn.clookie.design.ClockStyle
import io.github.harieedawwn.clookie.design.CornerStyle
import io.github.harieedawwn.clookie.design.CustomPreset
import io.github.harieedawwn.clookie.design.Look
import io.github.harieedawwn.clookie.design.UiDensity
import io.github.harieedawwn.clookie.model.Alarm
import io.github.harieedawwn.clookie.model.Days
import io.github.harieedawwn.clookie.model.MissionKeys
import io.github.harieedawwn.clookie.model.MissionSpec
import io.github.harieedawwn.clookie.model.MissionType
import io.github.harieedawwn.clookie.model.SleeperProfile
import io.github.harieedawwn.clookie.model.VibrationPattern
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** org.json is Android's, so this runs under Robolectric. */
@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {
    private val look = Look("socola", Color(0xFFE85D75), ClockFont.Ui, CornerStyle.Crisp, false, UiDensity.Compact)

    private val data = BackupData(
        alarms = listOf(
            Alarm(
                id = 3, hour = 6, minute = 30, days = Days.Weekdays, label = "Dậy đi học \"sớm\"; ~ ổn",
                sound = "content://media/x?y=1", volume = 95, rampSeconds = 60, vibration = VibrationPattern.Crescendo,
                snoozeMinutes = 5, snoozeLimit = 2,
                missions = listOf(
                    MissionSpec(MissionType.CookieCode, mapOf("code" to 7)),
                    MissionSpec(MissionType.Typing, mapOf(MissionKeys.Count to 1), "Chào buổi sáng; ~ Việt Nam!"),
                ),
                profile = SleeperProfile.Deep, skipDate = LocalDate.of(2026, 11, 4), enabled = false,
                volumeLock = true, snoozeAfterMission = true, awakeCheck = true, awakeCheckMinutes = 6,
            ),
            Alarm(id = 9, hour = 23, minute = 5),
        ),
        cookieCodes = listOf(CookieCodeEntity(7, "Phòng tắm", 0b101010001, 1_700_000_000_000)),
        scanCodes = listOf(ScanCodeEntity(2, "Dầu gội", "EAN_13", "8934563138165", 1_700_000_000_001)),
        settings = BackupSettings(
            look = look,
            customPresets = listOf(CustomPreset("c1", "Của tôi 1", look), CustomPreset("c2", "Tab\there", Look("dem"))),
            clockStyle = ClockStyle.Words, reducedMotion = true, sleepGoal = 450, bedtimeOn = true,
            defaultSound = "clookie:dawn",
        ),
        language = "en",
        createdAt = 1_762_000_000_000,
    )

    @Test
    fun everythingSurvivesARoundTrip() {
        val back = BackupCodec.decode(BackupCodec.encode(data))
        // Tabs in preset names are flattened by the preset codec.
        val expected = data.copy(
            settings = data.settings.copy(customPresets = listOf(data.settings.customPresets[0], CustomPreset("c2", "Tab here", Look("dem")))),
        )
        assertEquals(expected, back)
    }

    @Test
    fun schemaVersionIsWrittenAndChecked() {
        val json = JSONObject(BackupCodec.encode(data))
        assertEquals(BackupCodec.SchemaVersion, json.getInt("schema"))
        assertThrows(BackupException::class.java) { BackupCodec.decode(json.put("schema", BackupCodec.SchemaVersion + 1).toString()) }
        assertThrows(BackupException::class.java) { BackupCodec.decode("""{"app":"other","schema":1}""") }
        assertThrows(BackupException::class.java) { BackupCodec.decode("not json") }
    }

    @Test
    fun missingFieldsFallBackToDefaults() {
        val minimal = """{"app":"clookie","schema":1,"alarms":[{"id":1,"hour":7,"minute":15}]}"""
        val back = BackupCodec.decode(minimal)
        assertEquals(listOf(Alarm(id = 1, hour = 7, minute = 15)), back.alarms)
        assertEquals(BackupSettings(), back.settings)
    }
}
