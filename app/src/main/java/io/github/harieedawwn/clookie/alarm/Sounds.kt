package io.github.harieedawwn.clookie.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.model.Alarm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Clookie's seven synthesized sounds (scripts/sounds.py -> res/raw). */
enum class BuiltInSound(val key: String, @RawRes val raw: Int, @StringRes val nameRes: Int) {
    Crunch("crunch", R.raw.clookie_crunch, R.string.sound_crunch),
    Dawn("dawn", R.raw.clookie_dawn, R.string.sound_dawn),
    Chime("chime", R.raw.clookie_chime, R.string.sound_chime),
    Knock("knock", R.raw.clookie_knock, R.string.sound_knock),
    Chirp("chirp", R.raw.clookie_chirp, R.string.sound_chirp),
    Beeper("beeper", R.raw.clookie_beeper, R.string.sound_beeper),
    Siren("siren", R.raw.clookie_siren, R.string.sound_siren),
}

/** Where an alarm sound comes from. Stored as "clookie:<key>", "system:<uri>" or "file:<uri>". */
sealed interface SoundRef {
    val id: String

    data class BuiltIn(val sound: BuiltInSound) : SoundRef {
        override val id get() = "clookie:${sound.key}"
    }

    data class System(val uri: Uri) : SoundRef {
        override val id get() = "system:$uri"
    }

    data class File(val uri: Uri) : SoundRef {
        override val id get() = "file:$uri"
    }

    companion object {
        val Fallback = BuiltIn(BuiltInSound.Crunch)

        fun parse(id: String): SoundRef = when {
            id.startsWith("clookie:") -> BuiltInSound.entries.firstOrNull { it.key == id.removePrefix("clookie:") }
                ?.let { BuiltIn(it) } ?: Fallback
            id.startsWith("system:") -> System(Uri.parse(id.removePrefix("system:")))
            id.startsWith("file:") -> File(Uri.parse(id.removePrefix("file:")))
            else -> Fallback
        }
    }
}

data class SystemTone(val title: String, val uri: Uri)

object Sounds {
    val alarmAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /** A prepared, looping player on the alarm stream, or null if the source can't be read. */
    fun player(context: Context, ref: SoundRef): MediaPlayer? {
        val mp = MediaPlayer()
        return try {
            mp.setAudioAttributes(alarmAttributes)
            when (ref) {
                is SoundRef.BuiltIn -> context.resources.openRawResourceFd(ref.sound.raw).use {
                    mp.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                }
                is SoundRef.System -> mp.setDataSource(context, ref.uri)
                is SoundRef.File -> mp.setDataSource(context, ref.uri)
            }
            mp.isLooping = true
            mp.prepare()
            mp
        } catch (e: Exception) {
            mp.release()
            null
        }
    }

    /** Chosen sound, else the bundled crunch, else the system default alarm tone. */
    fun playerWithFallback(context: Context, id: String): MediaPlayer? =
        player(context, SoundRef.parse(id))
            ?: player(context, SoundRef.Fallback)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.let { player(context, SoundRef.System(it)) }

    suspend fun systemTones(context: Context): List<SystemTone> = withContext(Dispatchers.IO) {
        runCatching {
            val rm = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
            val cursor = rm.cursor ?: return@runCatching emptyList()
            buildList {
                while (cursor.moveToNext()) {
                    add(SystemTone(cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX), rm.getRingtoneUri(cursor.position)))
                }
            }
        }.getOrDefault(emptyList())
    }

    /** Display name for the sound tile; built-in names come from [BuiltInSound.nameRes]. */
    suspend fun displayName(context: Context, id: String): String = withContext(Dispatchers.IO) {
        when (val ref = SoundRef.parse(id)) {
            is SoundRef.BuiltIn -> context.getString(ref.sound.nameRes)
            is SoundRef.System -> runCatching { RingtoneManager.getRingtone(context, ref.uri)?.getTitle(context) }.getOrNull()
                ?: context.getString(R.string.sound_system)
            is SoundRef.File -> runCatching {
                context.contentResolver.query(ref.uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0)?.substringBeforeLast('.') else null
                }
            }.getOrNull() ?: context.getString(R.string.sound_file)
        }
    }

    fun isDefault(id: String) = id == Alarm.DefaultSound
}

/** Short preview while choosing a sound: plays a few seconds on the alarm stream, one at a time. */
object SoundPreview {
    private var player: MediaPlayer? = null
    private val stopper = android.os.Handler(android.os.Looper.getMainLooper())

    fun play(context: Context, id: String) {
        stop()
        player = Sounds.player(context.applicationContext, SoundRef.parse(id))?.apply {
            isLooping = false
            start()
        }
        stopper.postDelayed(::stop, 5000)
    }

    fun stop() {
        stopper.removeCallbacksAndMessages(null)
        player?.runCatching { stop(); release() }
        player = null
    }
}
