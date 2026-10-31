package io.github.harieedawwn.clookie.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.harieedawwn.clookie.model.Alarm
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
    private val defaultSoundKey = stringPreferencesKey("default_sound")
    private val lastCheckKey = longPreferencesKey("update_last_check")
    private val updateVersionKey = stringPreferencesKey("update_version")
    private val updateUrlKey = stringPreferencesKey("update_url")

    val presetId: Flow<String?> = data.data.map { it[presetKey] }
    val onboarded: Flow<Boolean> = data.data.map { it[onboardedKey] ?: false }
    val skippedPermissions: Flow<Set<String>> = data.data.map { it[skippedKey] ?: emptySet() }
    val autostartVisited: Flow<Boolean> = data.data.map { it[autostartKey] ?: false }
    val defaultSound: Flow<String> = data.data.map { it[defaultSoundKey] ?: Alarm.DefaultSound }
    val update: Flow<UpdateInfo?> = data.data.map { p ->
        val v = p[updateVersionKey]
        val u = p[updateUrlKey]
        if (v != null && u != null) UpdateInfo(v, u) else null
    }

    suspend fun presetNow(): String? = presetId.first()
    suspend fun lastUpdateCheck(): Long = data.data.first()[lastCheckKey] ?: 0L

    suspend fun setPreset(id: String) = data.edit { it[presetKey] = id }
    suspend fun setOnboarded() = data.edit { it[onboardedKey] = true }
    suspend fun setAutostartVisited() = data.edit { it[autostartKey] = true }
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
