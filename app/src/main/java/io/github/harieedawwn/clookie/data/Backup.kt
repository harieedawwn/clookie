package io.github.harieedawwn.clookie.data

import android.content.Context
import android.net.Uri
import io.github.harieedawwn.clookie.data.db.ClookieDatabase
import io.github.harieedawwn.clookie.data.db.CookieCodeEntity
import io.github.harieedawwn.clookie.data.db.ScanCodeEntity
import io.github.harieedawwn.clookie.design.ClockStyle
import io.github.harieedawwn.clookie.design.CustomPreset
import io.github.harieedawwn.clookie.design.Look
import io.github.harieedawwn.clookie.model.Alarm
import io.github.harieedawwn.clookie.model.AlarmKind
import io.github.harieedawwn.clookie.model.Days
import io.github.harieedawwn.clookie.model.MissionCodec
import io.github.harieedawwn.clookie.model.SleeperProfile
import io.github.harieedawwn.clookie.model.VibrationPattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class BackupSettings(
    val look: Look = Look(),
    val customPresets: List<CustomPreset> = emptyList(),
    val clockStyle: ClockStyle = ClockStyle.Digital,
    val reducedMotion: Boolean = false,
    val sleepGoal: Int = 480,
    val bedtimeOn: Boolean = false,
    val defaultSound: String = Alarm.DefaultSound,
)

/** Everything a backup file holds: regular alarms, their codes, settings and the app language. */
data class BackupData(
    val alarms: List<Alarm>,
    val cookieCodes: List<CookieCodeEntity>,
    val scanCodes: List<ScanCodeEntity>,
    val settings: BackupSettings,
    val language: String,
    val createdAt: Long = 0,
)

class BackupException(message: String) : Exception(message)

/**
 * JSON backup format. [SchemaVersion] goes up whenever a field changes meaning; older files
 * are read with defaults for missing fields, newer ones are refused. Ids are kept so missions
 * that point at a Cookie Code or QR code still find it after a restore.
 */
object BackupCodec {
    const val SchemaVersion = 1
    const val App = "clookie"

    fun encode(b: BackupData): String = JSONObject().apply {
        put("app", App)
        put("schema", SchemaVersion)
        put("createdAt", b.createdAt)
        put("language", b.language)
        put("settings", JSONObject().apply {
            put("look", b.settings.look.encode())
            put("customPresets", JSONArray(b.settings.customPresets.map { it.encode() }))
            put("clockStyle", b.settings.clockStyle.name)
            put("reducedMotion", b.settings.reducedMotion)
            put("sleepGoal", b.settings.sleepGoal)
            put("bedtimeOn", b.settings.bedtimeOn)
            put("defaultSound", b.settings.defaultSound)
        })
        put("alarms", JSONArray(b.alarms.map { it.toJson() }))
        put("cookieCodes", JSONArray(b.cookieCodes.map {
            JSONObject().put("id", it.id).put("name", it.name).put("pattern", it.pattern).put("createdAt", it.createdAt)
        }))
        put("scanCodes", JSONArray(b.scanCodes.map {
            JSONObject().put("id", it.id).put("name", it.name).put("format", it.format).put("value", it.value).put("createdAt", it.createdAt)
        }))
    }.toString(2)

    fun decode(text: String): BackupData {
        val o = runCatching { JSONObject(text) }.getOrElse { throw BackupException("not json") }
        if (o.optString("app") != App) throw BackupException("not a Clookie backup")
        val schema = o.optInt("schema", -1)
        if (schema < 1) throw BackupException("no schema")
        if (schema > SchemaVersion) throw BackupException("newer schema $schema")
        val s = o.optJSONObject("settings") ?: JSONObject()
        val defaults = BackupSettings()
        val settings = BackupSettings(
            look = Look.decode(s.optString("look")),
            customPresets = s.optJSONArray("customPresets").strings().let { CustomPreset.decodeAll(it.joinToString("\n")) },
            clockStyle = ClockStyle.entries.firstOrNull { it.name == s.optString("clockStyle") } ?: defaults.clockStyle,
            reducedMotion = s.optBoolean("reducedMotion", defaults.reducedMotion),
            sleepGoal = s.optInt("sleepGoal", defaults.sleepGoal),
            bedtimeOn = s.optBoolean("bedtimeOn", defaults.bedtimeOn),
            defaultSound = s.optString("defaultSound", defaults.defaultSound),
        )
        return BackupData(
            alarms = o.optJSONArray("alarms").objects().map { it.toAlarm() },
            cookieCodes = o.optJSONArray("cookieCodes").objects().map {
                CookieCodeEntity(it.getLong("id"), it.getString("name"), it.getInt("pattern"), it.optLong("createdAt"))
            },
            scanCodes = o.optJSONArray("scanCodes").objects().map {
                ScanCodeEntity(it.getLong("id"), it.getString("name"), it.getString("format"), it.getString("value"), it.optLong("createdAt"))
            },
            settings = settings,
            language = o.optString("language", "vi"),
            createdAt = o.optLong("createdAt"),
        )
    }

    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }
    private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else (0 until length()).map { getString(it) }

    private fun Alarm.toJson() = JSONObject().apply {
        put("id", id); put("hour", hour); put("minute", minute); put("days", days.mask); put("label", label)
        put("sound", sound); put("volume", volume); put("rampSeconds", rampSeconds); put("vibration", vibration.name)
        put("snoozeMinutes", snoozeMinutes); put("snoozeLimit", snoozeLimit); put("missions", MissionCodec.encode(missions))
        profile?.let { put("profile", it.name) }
        skipDate?.let { put("skipDate", it.toString()) }
        put("enabled", enabled); put("volumeLock", volumeLock); put("snoozeAfterMission", snoozeAfterMission)
        put("awakeCheck", awakeCheck); put("awakeCheckMinutes", awakeCheckMinutes)
    }

    /** Snooze and one-shot state is not restored: a restored alarm starts fresh. */
    private fun JSONObject.toAlarm(): Alarm {
        val d = Alarm(0, 0, 0)
        return Alarm(
            id = getLong("id"), hour = getInt("hour"), minute = getInt("minute"), days = Days(optInt("days", 0)),
            label = optString("label", ""), sound = optString("sound", d.sound), volume = optInt("volume", d.volume),
            rampSeconds = optInt("rampSeconds", d.rampSeconds),
            vibration = VibrationPattern.entries.firstOrNull { it.name == optString("vibration") } ?: d.vibration,
            snoozeMinutes = optInt("snoozeMinutes", d.snoozeMinutes), snoozeLimit = optInt("snoozeLimit", d.snoozeLimit),
            missions = MissionCodec.decode(optString("missions", "")),
            profile = SleeperProfile.entries.firstOrNull { it.name == optString("profile") },
            skipDate = optString("skipDate").takeIf { it.isNotEmpty() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            enabled = optBoolean("enabled", true), kind = AlarmKind.Alarm,
            volumeLock = optBoolean("volumeLock", false), snoozeAfterMission = optBoolean("snoozeAfterMission", false),
            awakeCheck = optBoolean("awakeCheck", false), awakeCheckMinutes = optInt("awakeCheckMinutes", d.awakeCheckMinutes),
        )
    }
}

/** Reads and writes backup files picked through the system file picker. */
object Backups {
    suspend fun export(context: Context, uri: Uri, language: String): Int = withContext(Dispatchers.IO) {
        val db = ClookieDatabase.get(context)
        val alarms = db.alarms().all().map { it.toModel() }.filter { it.kind == AlarmKind.Alarm }
        val data = BackupData(
            alarms, db.codes().cookieCodes(), db.codes().scanCodes(), SettingsStore(context).snapshot(), language,
            System.currentTimeMillis(),
        )
        context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(BackupCodec.encode(data).toByteArray(Charsets.UTF_8)) }
        alarms.size
    }

    /** Replaces alarms, codes and settings with the file's. Returns the decoded data (for the language). */
    suspend fun import(context: Context, uri: Uri): BackupData = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: throw BackupException("unreadable")
        val data = BackupCodec.decode(text)
        AlarmRepository.get(context).replaceAll(data.alarms, data.cookieCodes, data.scanCodes)
        SettingsStore(context).restore(data.settings)
        data
    }
}
