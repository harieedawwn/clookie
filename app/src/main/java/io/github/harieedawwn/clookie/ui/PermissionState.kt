package io.github.harieedawwn.clookie.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.harieedawwn.clookie.data.SettingsStore
import io.github.harieedawwn.clookie.reliability.PermItem
import io.github.harieedawwn.clookie.reliability.PermRow
import io.github.harieedawwn.clookie.reliability.PermState
import io.github.harieedawwn.clookie.reliability.Permissions
import kotlinx.coroutines.launch

/**
 * Live permission rows, re-checked every time the app resumes (returning from a system page).
 * Shared by the permission screen, Độ tin cậy and the Home dot.
 */
@Composable
fun rememberPermissionRows(store: SettingsStore): State<List<PermRow>> {
    val context = LocalContext.current
    val skipped by store.skippedPermissions.collectAsState(initial = emptySet())
    val visited by store.autostartVisited.collectAsState(initial = false)
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose { }
    }
    return produceState(Permissions.rows(context, skipped, visited), resumes, skipped, visited) {
        value = Permissions.rows(context, skipped, visited)
    }
}

/**
 * Grant actions: runtime dialogs or system settings pages; "Cấp tất cả" walks the missing
 * rows one after another, continuing each time the user comes back.
 */
class PermissionActions(
    val grant: (PermItem) -> Unit,
    val grantAll: (List<PermRow>) -> Unit,
    val skip: (PermItem) -> Unit,
)

@Composable
fun rememberPermissionActions(store: SettingsStore): PermissionActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var queue by remember { mutableStateOf(emptyList<PermItem>()) }
    var waitingResume by remember { mutableStateOf(false) }
    var next: () -> Unit = {}

    val runtime = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val item = queue.firstOrNull()
        if (!granted && item != null) {
            // Denied for good: the dialog no longer shows, so offer the settings page instead.
            val perm = Permissions.runtimePermission(item)
            val activity = context.findActivity()
            if (perm != null && activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)) {
                Permissions.openSettings(context, item)
                waitingResume = true
                return@rememberLauncherForActivityResult
            }
        }
        queue = queue.drop(1)
        next()
    }

    fun launch(item: PermItem) {
        val perm = Permissions.runtimePermission(item)
        if (perm != null) {
            runtime.launch(perm)
        } else {
            if (item == PermItem.Autostart) scope.launch { store.setAutostartVisited() }
            if (Permissions.openSettings(context, item)) waitingResume = true else {
                queue = queue.drop(1)
                next()
            }
        }
    }
    next = { queue.firstOrNull()?.let(::launch) }

    LifecycleResumeEffect(Unit) {
        if (waitingResume) {
            waitingResume = false
            queue = queue.drop(1)
            next()
        }
        onPauseOrDispose { }
    }

    return remember {
        PermissionActions(
            grant = { item ->
                queue = listOf(item)
                launch(item)
            },
            grantAll = { rows ->
                queue = rows.filter { it.state != PermState.Granted }.map { it.item }
                next()
            },
            skip = { item -> scope.launch { store.setSkipped(item.name, true) } },
        )
    }
}

internal fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/**
 * Inline runtime-permission request for features that need one (a mission's camera). Returns a
 * launcher taking the permissions; [onResult] gets true when all are granted. When a permission
 * is denied for good, its settings page opens instead, like on the permission screen.
 */
@Composable
fun rememberPermissionRequest(onResult: (Boolean) -> Unit): (List<String>) -> Unit {
    val context = LocalContext.current
    val result by rememberUpdatedState(onResult)
    var asked by remember { mutableStateOf(emptyList<String>()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        val ok = asked.all { granted[it] == true }
        val activity = context.findActivity()
        if (!ok && activity != null) {
            val forGood = asked.firstOrNull { granted[it] != true && !ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
            PermItem.entries.firstOrNull { forGood != null && Permissions.runtimePermission(it) == forGood }
                ?.let { Permissions.openSettings(context, it) }
        }
        result(ok)
    }
    return { permissions ->
        val missing = permissions.filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) {
            result(true)
        } else {
            asked = missing
            launcher.launch(missing.toTypedArray())
        }
    }
}
