package io.github.harieedawwn.clookie.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.github.harieedawwn.clookie.BuildConfig
import io.github.harieedawwn.clookie.data.SettingsStore
import io.github.harieedawwn.clookie.data.UpdateInfo
import io.github.harieedawwn.clookie.model.isNewerVersion
import io.github.harieedawwn.clookie.reliability.PermItem
import io.github.harieedawwn.clookie.reliability.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Updates from GitHub Releases: the latest release's APK asset, checked at most once a day
 * (or from Settings), downloaded to cache and handed to the system installer.
 */
object Updater {
    private const val LatestUrl = "https://api.github.com/repos/harieedawwn/clookie/releases/latest"
    private const val DayMs = 24 * 60 * 60 * 1000L

    /** Newer release, or null. Without [force] it reuses a check from the last 24 h. */
    suspend fun check(context: Context, force: Boolean): UpdateInfo? {
        val store = SettingsStore(context)
        val now = System.currentTimeMillis()
        if (!force && now - store.lastUpdateCheck() < DayMs) {
            return store.update.first()?.takeIf { isNewerVersion(it.version, BuildConfig.VERSION_NAME) }
        }
        val latest = fetchLatest() ?: return if (force) null else store.update.first()
        val newer = latest.takeIf { isNewerVersion(it.version, BuildConfig.VERSION_NAME) }
        store.setUpdateCheck(now, newer)
        return newer
    }

    private suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(LatestUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Clookie/${BuildConfig.VERSION_NAME}")
            }
            try {
                if (conn.responseCode != 200) return@runCatching null
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val tag = json.getString("tag_name")
                val assets = json.getJSONArray("assets")
                val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
                    .firstOrNull { it.getString("name").endsWith(".apk") } ?: return@runCatching null
                UpdateInfo(tag.removePrefix("v"), apk.getString("browser_download_url"))
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    /** Downloads the APK, reporting progress 0..1; returns the file or null on failure. */
    suspend fun download(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File? = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, "Clookie-v${info.version}.apk")
        runCatching {
            val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Clookie/${BuildConfig.VERSION_NAME}")
            }
            try {
                if (conn.responseCode != 200) return@runCatching null
                val total = conn.contentLengthLong
                conn.inputStream.use { input ->
                    out.outputStream().use { output ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        var lastReport = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            output.write(buf, 0, n)
                            done += n
                            if (total > 0 && done - lastReport > 128 * 1024) {
                                lastReport = done
                                withContext(Dispatchers.Main) { onProgress(done.toFloat() / total) }
                            }
                        }
                    }
                }
                out
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    /** Opens the system installer, or the "install unknown apps" page if that is not allowed yet. */
    fun install(context: Context, apk: File): Boolean {
        if (!Permissions.isGranted(context, PermItem.InstallUpdates, emptySet())) {
            Permissions.openSettings(context, PermItem.InstallUpdates)
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }
}
