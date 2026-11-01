package io.github.harieedawwn.clookie.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.harieedawwn.clookie.design.ClockStyle
import io.github.harieedawwn.clookie.design.CustomPreset
import io.github.harieedawwn.clookie.design.Look
import io.github.harieedawwn.clookie.model.Alarm
import io.github.harieedawwn.clookie.model.Bedtime
import io.github.harieedawwn.clookie.model.Stopwatch
import io.github.harieedawwn.clookie.model.TimerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Device-protected storage: readable before the first unlock after a reboot. */
fun Context.deviceStorage(): Context = applicationContext.createDeviceProtectedStorageContext()

private object SettingsFile {
    @Volatile private var store: DataStore<Preferences>? = null

    fun get(context: Context): DataStore<Preferences> = store ?: synchronized(this) {
        store ?: PreferenceDataStoreFactory.create {
            context.deviceStorage().preferencesDataStoreFile("settings")
        }.also { store = it }
    }
}

/** A published release the updater found. */
data class UpdateInfo(val version: String, val apkUrl: String)

/** App settings (DataStore). Language lives in AppCompat's per-app locale instead. */
class SettingsStore(context: Context) {
    private val data = SettingsFile.get(context)

    private val presetKey = stringPreferencesKey("preset")
    private val onboardedKey = booleanPreferencesKey("onboarded")
    private val skippedKey = stringSetPreferencesKey("skipped_permissions")
    private val autostartKey = booleanPreferencesKey("autostart_visited")
    private val visitedKey = stringSetPreferencesKey("visited_pages")
    private val defaultSoundKey = stringPreferencesKey("default_sound")
    private val lastCheckKey = longPreferencesKey("update_last_check")
    private val updateVersionKey = stringPreferencesKey("update_version")
    private val updateUrlKey = stringPreferencesKey("update_url")
    private val lookKey = stringPreferencesKey("look")
    private val customKey = stringPreferencesKey("custom_presets")
    private val clockStyleKey = stringPreferencesKey("clock_style")
    private val reducedMotionKey = booleanPreferencesKey("reduced_motion")
    private val sleepGoalKey = intPreferencesKey("sleep_goal")
    private val bedtimeKey = booleanPreferencesKey("bedtime_on")
    private val timerKey = stringPreferencesKey("timer")
    private val stopwatchKey = stringPreferencesKey("stopwatch")

    val presetId: Flow<String?> = data.data.map { it[presetKey] }
    val onboarded: Flow<Boolean> = data.data.map { it[onboardedKey] ?: false }
    val skippedPermissions: Flow<Set<String>> = data.data.map { it[skippedKey] ?: emptySet() }
    /** Permission rows (PermItem names) whose unreadable settings page was opened. */
    val visitedPages: Flow<Set<String>> = data.data.map {
        (it[visitedKey] ?: emptySet()) + (if (it[autostartKey] == true) setOf("Autostart") else emptySet())
    }
    val defaultSound: Flow<String> = data.data.map { it[defaultSoundKey] ?: Alarm.DefaultSound }
    val update: Flow<UpdateInfo?> = data.data.map { p ->
        val v = p[updateVersionKey]
        val u = p[updateUrlKey]
        if (v != null && u != null) UpdateInfo(v, u) else null
    }

    /** Active look; before Stage 5 only the preset id was stored. */
    val look: Flow<Look> = data.data.map { Look.decode(it[lookKey], it[presetKey]) }
    val customPresets: Flow<List<CustomPreset>> = data.data.map { CustomPreset.decodeAll(it[customKey]) }
    val clockStyle: Flow<ClockStyle> = data.data.map { p -> ClockStyle.entries.firstOrNull { it.name == p[clockStyleKey] } ?: ClockStyle.Digital }
    val reducedMotion: Flow<Boolean> = data.data.map { it[reducedMotionKey] ?: false }
    val sleepGoal: Flow<Int> = data.data.map { it[sleepGoalKey] ?: Bedtime.DefaultGoal }
    val bedtimeOn: Flow<Boolean> = data.data.map { it[bedtimeKey] ?: false }
    val timer: Flow<TimerState> = data.data.map { TimerState.decode(it[timerKey]) }
    val stopwatch: Flow<Stopwatch> = data.data.map { Stopwatch.decode(it[stopwatchKey]) }

    suspend fun presetNow(): String? = presetId.first()
    suspend fun lookNow(): Look = look.first()
    suspend fun reducedMotionNow(): Boolean = reducedMotion.first()

    suspend fun setLook(look: Look) = data.edit {
        it[lookKey] = look.encode()
        it[presetKey] = look.base
    }
    suspend fun setCustomPresets(list: List<CustomPreset>) = data.edit { it[customKey] = CustomPreset.encodeAll(list) }
    suspend fun setClockStyle(style: ClockStyle) = data.edit { it[clockStyleKey] = style.name }
    suspend fun setReducedMotion(on: Boolean) = data.edit { it[reducedMotionKey] = on }
    suspend fun setSleepGoal(minutes: Int) = data.edit { it[sleepGoalKey] = minutes }
    suspend fun setBedtime(on: Boolean) = data.edit { it[bedtimeKey] = on }
    suspend fun setTimer(t: TimerState) = data.edit { it[timerKey] = t.encode() }
    suspend fun setStopwatch(s: Stopwatch) = data.edit { it[stopwatchKey] = s.encode() }

    /** Settings that go into a backup file. */
    suspend fun snapshot(): BackupSettings = data.data.first().let { p ->
        BackupSettings(
            look = Look.decode(p[lookKey], p[presetKey]),
            customPresets = CustomPreset.decodeAll(p[customKey]),
            clockStyle = ClockStyle.entries.firstOrNull { it.name == p[clockStyleKey] } ?: ClockStyle.Digital,
            reducedMotion = p[reducedMotionKey] ?: false,
            sleepGoal = p[sleepGoalKey] ?: Bedtime.DefaultGoal,
            bedtimeOn = p[bedtimeKey] ?: false,
            defaultSound = p[defaultSoundKey] ?: Alarm.DefaultSound,
        )
    }

    suspend fun restore(b: BackupSettings) = data.edit {
        it[lookKey] = b.look.encode()
        it[presetKey] = b.look.base
        it[customKey] = CustomPreset.encodeAll(b.customPresets)
        it[clockStyleKey] = b.clockStyle.name
        it[reducedMotionKey] = b.reducedMotion
        it[sleepGoalKey] = b.sleepGoal
        it[bedtimeKey] = b.bedtimeOn
        it[defaultSoundKey] = b.defaultSound
    }
    suspend fun lastUpdateCheck(): Long = data.data.first()[lastCheckKey] ?: 0L

    suspend fun setOnboarded() = data.edit { it[onboardedKey] = true }
    suspend fun setVisited(item: String) = data.edit { it[visitedKey] = (it[visitedKey] ?: emptySet()) + item }
    suspend fun setDefaultSound(id: String) = data.edit { it[defaultSoundKey] = id }

    suspend fun setSkipped(item: String, skipped: Boolean) = data.edit {
        val now = it[skippedKey] ?: emptySet()
        it[skippedKey] = if (skipped) now + item else now - item
    }

    suspend fun setUpdateCheck(at: Long, found: UpdateInfo?) = data.edit {
        it[lastCheckKey] = at
        if (found != null) {
            it[updateVersionKey] = found.version
            it[updateUrlKey] = found.apkUrl
        } else {
            it.remove(updateVersionKey)
            it.remove(updateUrlKey)
        }
    }
}
