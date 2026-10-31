package io.github.harieedawwn.clookie.data.db

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import io.github.harieedawwn.clookie.data.deviceStorage
import kotlinx.coroutines.flow.Flow

/**
 * Alarm storage. Schema v1 is frozen as of Stage 2 (exported to app/schemas); every later
 * change bumps the version and adds a Migration, never a destructive fallback.
 * Missions use [io.github.harieedawwn.clookie.model.MissionCodec]; instants are epoch ms.
 */
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val daysMask: Int,
    val label: String,
    val sound: String,
    val volume: Int,
    val rampSeconds: Int,
    val vibration: String,
    val snoozeMinutes: Int,
    val snoozeLimit: Int,
    val missions: String,
    val profile: String?,
    /** Epoch day of the skipped occurrence ("Bỏ qua lần tới"). */
    val skipDate: Long?,
    val enabled: Boolean,
    @ColumnInfo(defaultValue = "Alarm") val kind: String,
    val oneShotAt: Long?,
    val snoozedUntil: Long?,
    @ColumnInfo(defaultValue = "0") val snoozeCount: Int,
    @ColumnInfo(defaultValue = "0") val volumeLock: Boolean,
    @ColumnInfo(defaultValue = "0") val snoozeAfterMission: Boolean,
    @ColumnInfo(defaultValue = "0") val awakeCheck: Boolean,
    @ColumnInfo(defaultValue = "4") val awakeCheckMinutes: Int,
    /** Instant registered with AlarmManager and not fired yet; detects alarms missed while off. */
    val pendingTriggerAt: Long?,
    @ColumnInfo(defaultValue = "0") val createdAt: Long,
)

/** Stage 3: hand-drawn Cookie Codes (3x3 pattern as a 9-bit mask). */
@Entity(tableName = "cookie_codes")
data class CookieCodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val pattern: Int,
    val createdAt: Long,
)

/** Stage 3: registered QR codes and barcodes. */
@Entity(tableName = "scan_codes")
data class ScanCodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val format: String,
    val value: String,
    val createdAt: Long,
)

/** One finished ring (Stage 5 shows the log, streak and chart). */
@Entity(tableName = "wake_log", indices = [Index("endedAt")])
data class WakeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alarmId: Long,
    val label: String,
    val scheduledAt: Long,
    val firedAt: Long,
    val endedAt: Long,
    val outcome: String,
    val snoozeCount: Int,
    val missions: String,
)

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute, id")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms")
    suspend fun all(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun get(id: Long): AlarmEntity?

    @Upsert
    suspend fun upsert(alarm: AlarmEntity): Long

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE alarms SET pendingTriggerAt = :at WHERE id = :id")
    suspend fun setPending(id: Long, at: Long?)

    @Insert
    suspend fun log(entry: WakeLogEntity)
}

@Dao
interface CodeDao {
    @Query("SELECT * FROM cookie_codes ORDER BY createdAt")
    fun observeCookieCodes(): Flow<List<CookieCodeEntity>>

    @Query("SELECT * FROM cookie_codes ORDER BY createdAt")
    suspend fun cookieCodes(): List<CookieCodeEntity>

    @Insert
    suspend fun insertCookieCode(code: CookieCodeEntity): Long

    @Query("DELETE FROM cookie_codes WHERE id = :id")
    suspend fun deleteCookieCode(id: Long)

    @Query("SELECT * FROM scan_codes ORDER BY createdAt")
    fun observeScanCodes(): Flow<List<ScanCodeEntity>>

    @Query("SELECT * FROM scan_codes ORDER BY createdAt")
    suspend fun scanCodes(): List<ScanCodeEntity>

    @Insert
    suspend fun insertScanCode(code: ScanCodeEntity): Long

    @Query("DELETE FROM scan_codes WHERE id = :id")
    suspend fun deleteScanCode(id: Long)
}

@Database(
    entities = [AlarmEntity::class, CookieCodeEntity::class, ScanCodeEntity::class, WakeLogEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ClookieDatabase : RoomDatabase() {
    abstract fun alarms(): AlarmDao

    abstract fun codes(): CodeDao

    companion object {
        @Volatile private var instance: ClookieDatabase? = null

        /** Lives in device-protected storage so alarms ring after a reboot before the first unlock. */
        fun get(context: Context): ClookieDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.deviceStorage(), ClookieDatabase::class.java, "clookie.db")
                .build().also { instance = it }
        }
    }
}
