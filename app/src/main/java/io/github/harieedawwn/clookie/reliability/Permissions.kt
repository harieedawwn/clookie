package io.github.harieedawwn.clookie.reliability

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.harieedawwn.clookie.MainActivity
import io.github.harieedawwn.clookie.alarm.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Rows of the permission screen and Độ tin cậy. [critical] items light the dot on Home. */
enum class PermItem(val required: Boolean, val critical: Boolean = false) {
    Notifications(true, critical = true),
    ExactAlarm(true, critical = true),
    FullScreen(true, critical = true),
    // Without these the alarm rings but its screen only shows after unlocking (phone in use, MIUI).
    Overlay(true, critical = true),
    LockScreen(true),
    Battery(true),
    Autostart(true),
    Camera(false),
    Activity(false),
    InstallUpdates(false),
}

/** Rows that no API can read: they count as done once their settings page was opened. */
val PermItem.visitOnly: Boolean get() = this == PermItem.Autostart || this == PermItem.LockScreen

enum class PermState { Granted, Missing, Skipped }

data class PermRow(val item: PermItem, val state: PermState)

/** Statuses from granted flags (pure; the screens and tests use this). */
fun permRows(items: List<PermItem>, granted: (PermItem) -> Boolean, skipped: Set<String>): List<PermRow> =
    items.map { item ->
        PermRow(
            item,
            when {
                granted(item) -> PermState.Granted
                item.required && item.name in skipped -> PermState.Skipped
                else -> PermState.Missing
            },
        )
    }

/** Something that makes alarms unreliable is off (Home dot). */
fun List<PermRow>.criticalIssue(): Boolean = any { it.item.critical && it.state != PermState.Granted }

/** Required rows are all granted or explicitly skipped ("Xong" enabled). */
fun List<PermRow>.requiredSettled(): Boolean = none { it.item.required && it.state == PermState.Missing }

object Permissions {
    val oem: Oem? get() = Oem.detect(Build.MANUFACTURER ?: "", Build.BRAND ?: "")

    /** Rows that apply to this device and API level. */
    fun items(): List<PermItem> = PermItem.entries.filter { item ->
        when (item) {
            PermItem.Autostart -> oem != null
            PermItem.LockScreen -> oem?.lockScreenPages?.isNotEmpty() == true
            PermItem.Activity -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            else -> true
        }
    }

    fun isGranted(context: Context, item: PermItem, visited: Set<String>): Boolean = when (item) {
        PermItem.Notifications -> NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            context.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(Notifications.CHANNEL_RINGING)?.importance != NotificationManager.IMPORTANCE_NONE
        PermItem.ExactAlarm -> Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        PermItem.FullScreen -> Build.VERSION.SDK_INT < 34 ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        PermItem.Battery -> context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
        // No API reports OEM autostart or lock-screen pop-ups; the row counts as done once its page was opened.
        PermItem.Autostart, PermItem.LockScreen -> item.name in visited
        PermItem.Camera -> has(context, Manifest.permission.CAMERA)
        PermItem.Activity -> Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || has(context, Manifest.permission.ACTIVITY_RECOGNITION)
        PermItem.InstallUpdates -> context.packageManager.canRequestPackageInstalls()
        PermItem.Overlay -> Settings.canDrawOverlays(context)
    }

    fun rows(context: Context, skipped: Set<String>, visited: Set<String>): List<PermRow> =
        permRows(items(), { isGranted(context, it, visited) }, skipped)

    /** Runtime permission behind a row, if it is granted by a dialog rather than a settings page. */
    fun runtimePermission(item: PermItem): String? = when (item) {
        PermItem.Notifications -> if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS else null
        PermItem.Camera -> Manifest.permission.CAMERA
        PermItem.Activity -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACTIVITY_RECOGNITION else null
        else -> null
    }

    /** Settings pages that fix [item], best first; the app details page is always last. */
    fun settingsIntents(context: Context, item: PermItem): List<Intent> {
        val pkg = context.packageName
        val pkgUri = Uri.parse("package:$pkg")
        val primary = when (item) {
            PermItem.Notifications -> listOf(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg))
            PermItem.ExactAlarm -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listOf(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkgUri))
            } else emptyList()
            PermItem.FullScreen -> if (Build.VERSION.SDK_INT >= 34) {
                listOf(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkgUri))
            } else emptyList()
            @Suppress("BatteryLife")
            PermItem.Battery -> listOf(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkgUri),
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            )
            PermItem.Autostart -> oem?.pages.orEmpty().map { (p, c) -> Intent().setComponent(ComponentName(p, c)) }
            PermItem.LockScreen -> oem?.lockScreenPages.orEmpty().map { (p, c) ->
                Intent("miui.intent.action.APP_PERM_EDITOR").setComponent(ComponentName(p, c)).putExtra("extra_pkgname", pkg)
            }
            PermItem.InstallUpdates -> listOf(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, pkgUri))
            PermItem.Overlay -> listOf(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri))
            PermItem.Camera, PermItem.Activity -> emptyList()
        }
        return primary + Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri)
    }

    /**
     * Opens the first settings page that exists. While it is open, watches the status and
     * brings Clookie back as soon as the permission is granted.
     */
    fun openSettings(context: Context, item: PermItem): Boolean {
        for (intent in settingsIntents(context, item)) {
            try {
                // No resolveActivity check: package visibility hides OEM pages; a missing page throws.
                context.startActivity(intent)
                watch(context.applicationContext, item)
                return true
            } catch (e: ActivityNotFoundException) {
                continue
            } catch (e: SecurityException) {
                continue
            }
        }
        return false
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watcher: Job? = null

    private fun watch(app: Context, item: PermItem) {
        watcher?.cancel()
        // Autostart and runtime-only rows cannot be observed; the user comes back with Back.
        if (item.visitOnly) return
        val before = isGranted(app, item, emptySet())
        if (before) return
        watcher = scope.launch {
            repeat(240) {
                delay(500)
                if (isGranted(app, item, emptySet())) {
                    app.startActivity(
                        Intent(app, MainActivity::class.java).addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
                        ),
                    )
                    return@launch
                }
            }
        }
    }

    private fun has(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
