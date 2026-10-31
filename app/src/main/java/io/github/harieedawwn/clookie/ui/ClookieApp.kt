package io.github.harieedawwn.clookie.ui

import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import io.github.harieedawwn.clookie.BuildConfig
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.data.AlarmRepository
import io.github.harieedawwn.clookie.data.SettingsStore
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.ClookieTheme
import io.github.harieedawwn.clookie.design.Presets
import io.github.harieedawwn.clookie.model.Alarm
import io.github.harieedawwn.clookie.reliability.criticalIssue
import io.github.harieedawwn.clookie.ui.components.NavDest
import io.github.harieedawwn.clookie.ui.screens.AlarmEditorScreen
import io.github.harieedawwn.clookie.ui.screens.HomeScreen
import io.github.harieedawwn.clookie.ui.screens.MainScaffold
import io.github.harieedawwn.clookie.ui.screens.PermissionScreen
import io.github.harieedawwn.clookie.ui.screens.ReliabilityScreen
import io.github.harieedawwn.clookie.ui.screens.RingingScreen
import io.github.harieedawwn.clookie.ui.screens.SettingsScreen
import io.github.harieedawwn.clookie.ui.screens.StopwatchScreen
import io.github.harieedawwn.clookie.ui.screens.StudioScreen
import io.github.harieedawwn.clookie.ui.screens.TimerScreen
import io.github.harieedawwn.clookie.ui.screens.WakeLogScreen
import io.github.harieedawwn.clookie.alarm.AlarmSideEffects
import io.github.harieedawwn.clookie.data.Backups
import io.github.harieedawwn.clookie.design.ClockStyle
import io.github.harieedawwn.clookie.design.CustomPreset
import io.github.harieedawwn.clookie.design.Look
import io.github.harieedawwn.clookie.model.AlarmKind
import io.github.harieedawwn.clookie.model.Bedtime
import io.github.harieedawwn.clookie.model.Stopwatch
import io.github.harieedawwn.clookie.model.TimerState
import io.github.harieedawwn.clookie.model.WakeStats
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface Route {
    data object Main : Route
    data class Editor(val alarmId: Long?) : Route
    data class Ringing(val alarm: Alarm) : Route
    data object Themes : Route
    data class Permissions(val firstLaunch: Boolean) : Route
    data object Reliability : Route
    data object WakeLog : Route
}

/** App shell: Room-backed alarms, first-launch permission screen, Độ tin cậy, updater. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ClookieApp(
    store: SettingsStore,
    repo: AlarmRepository,
    window: Window?,
    initialPreset: String?,
    onboarded: Boolean,
    initialReducedMotion: Boolean = false,
) {
    val look by store.look.collectAsState(initial = Look(Presets.byId(initialPreset).id))
    val preset = Presets.byId(look.base)
    val customPresets by store.customPresets.collectAsState(initial = emptyList())
    val clockStyle by store.clockStyle.collectAsState(initial = ClockStyle.Digital)
    val reducedMotion by store.reducedMotion.collectAsState(initial = initialReducedMotion)
    val sleepGoal by store.sleepGoal.collectAsState(initial = Bedtime.DefaultGoal)
    val bedtimeOn by store.bedtimeOn.collectAsState(initial = false)
    val timer by store.timer.collectAsState(initial = TimerState())
    val stopwatch by store.stopwatch.collectAsState(initial = Stopwatch())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val allAlarms by repo.alarms.collectAsState(initial = emptyList())
    // Timers ring through the engine but are not alarms on Home.
    val alarms = remember(allAlarms) { allAlarms.filter { it.kind != AlarmKind.Timer } }
    val wakeLog by remember { repo.wakeLog(System.currentTimeMillis() - 60L * 86_400_000L) }.collectAsState(initial = emptyList())
    val streak = remember(wakeLog) { WakeStats.streak(wakeLog, ZoneId.systemDefault()) }
    // Widget colors and the bedtime reminder follow the look and the sleep settings.
    LaunchedEffect(look, bedtimeOn, sleepGoal) { AlarmSideEffects.refresh(context.applicationContext) }
    var backupStatus by remember { mutableStateOf<String?>(null) }
    val backupSaved = stringResource(R.string.backup_done)
    val restoreDone = stringResource(R.string.restore_done)
    val backupFailed = stringResource(R.string.backup_failed)
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            backupStatus = runCatching { Backups.export(context, uri, currentLanguage()) }
                .fold({ backupSaved.format(it) }, { backupFailed })
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            backupStatus = runCatching { Backups.import(context, uri) }.fold({ data ->
                if (data.language != currentLanguage()) AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(data.language))
                restoreDone.format(data.alarms.size)
            }, { backupFailed })
        }
    }
    val timerLabel = stringResource(R.string.nav_timer)
    fun timerStart() = scope.launch {
        if (timer.running) return@launch
        val now = System.currentTimeMillis()
        val endAt = now + timer.left(now)
        val id = repo.addTimer(endAt, timerLabel, Alarm(0, 0, 0, sound = store.defaultSound.first()))
        store.setTimer(timer.copy(endAt = endAt, remaining = null, alarmId = id))
    }
    fun timerPause() = scope.launch {
        timer.alarmId?.let { repo.delete(it) }
        store.setTimer(timer.copy(endAt = null, remaining = timer.left(System.currentTimeMillis()), alarmId = null))
    }
    fun timerReset(duration: Long = timer.duration) = scope.launch {
        timer.alarmId?.let { repo.delete(it) }
        store.setTimer(TimerState(duration))
    }
    fun stopwatch(change: (Stopwatch, Long) -> Stopwatch) = scope.launch {
        store.setStopwatch(change(stopwatch, SystemClock.elapsedRealtime()))
    }
    val defaultSound by store.defaultSound.collectAsState(initial = Alarm.DefaultSound)
    val rows by rememberPermissionRows(store)
    val permissions = rememberPermissionActions(store)
    val updates = remember { UpdateController(context.applicationContext, scope) }
    LaunchedEffect(Unit) { updates.check(force = false) }

    val stack = remember { mutableStateListOf(if (onboarded) Route.Main else Route.Permissions(firstLaunch = true)) }
    var tab by rememberSaveable { mutableStateOf(NavDest.Alarms) }
    val ticker = rememberTicker()
    val route = stack.last()
    BackHandler(stack.size > 1) { stack.removeAt(stack.lastIndex) }
    fun go(r: Route) = stack.add(r)
    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    val issue = rows.criticalIssue()
    val napLabel = stringResource(R.string.nap_label)

    ClookieTheme(preset, reducedMotion = reducedMotion, look = look) {
        val c = Clookie.colors
        val motion = Clookie.motion
        val view = LocalView.current
        val lightBars = if (route is Route.Ringing) c.ringing.luminance() > 0.5f else !c.isDark
        SideEffect {
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
        }
        CompositionLocalProvider(LocalNow provides ticker) {
            SharedTransitionLayout {
                AnimatedContent(
                    targetState = route,
                    transitionSpec = { fadeIn(motion.standard()) togetherWith fadeOut(motion.standard()) },
                    label = "route",
                ) { r ->
                    CompositionLocalProvider(LocalSharedScope provides this@SharedTransitionLayout, LocalAnimatedScope provides this) {
                        when (r) {
                            Route.Main -> MainScaffold(tab, { tab = it }) {
                                when (tab) {
                                    NavDest.Alarms -> HomeScreen(
                                        alarms = alarms,
                                        onToggle = { a, on -> scope.launch { repo.setEnabled(a, on) } },
                                        onOpen = { go(Route.Editor(it.id)) },
                                        onAdd = { go(Route.Editor(null)) },
                                        onNap = { m -> scope.launch { repo.addNap(m, napLabel, Alarm(0, 0, 0, sound = defaultSound)) } },
                                        onReliability = { go(Route.Reliability) },
                                        reliabilityIssue = issue,
                                        banner = if (updates.state.info() != null) {
                                            { UpdateBanner(updates.state, updates::act) }
                                        } else null,
                                        clockStyle = clockStyle,
                                    )
                                    NavDest.Timer -> TimerScreen(
                                        timer,
                                        onPick = { ms -> if (!timer.running || timer.left(System.currentTimeMillis()) == 0L) timerReset(ms) },
                                        onStart = { timerStart() },
                                        onPause = { timerPause() },
                                        onReset = { timerReset() },
                                    )
                                    NavDest.Stopwatch -> StopwatchScreen(
                                        remember(stopwatch) { stopwatch.afterReboot(SystemClock.elapsedRealtime()) },
                                        onStart = { stopwatch { s, now -> s.afterReboot(now).start(now) } },
                                        onPause = { stopwatch { s, now -> s.afterReboot(now).pause(now) } },
                                        onLap = { stopwatch { s, now -> s.afterReboot(now).lap(now) } },
                                        onReset = { stopwatch { s, _ -> s.reset() } },
                                    )
                                    NavDest.Settings -> SettingsScreen(
                                        preset = preset,
                                        language = currentLanguage(),
                                        versionName = BuildConfig.VERSION_NAME,
                                        onLanguage = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(it)) },
                                        onTheme = { go(Route.Themes) },
                                        onReliability = { go(Route.Reliability) },
                                        update = updates.state,
                                        onUpdate = updates::act,
                                        reliabilityIssue = issue,
                                        defaultSound = defaultSound,
                                        onDefaultSound = { scope.launch { store.setDefaultSound(it) } },
                                        themeName = customPresets.firstOrNull { it.look == look }?.name
                                            ?: if (look.isPlain) null else stringResource(preset.nameRes) + " · " + stringResource(R.string.custom),
                                        reducedMotion = reducedMotion,
                                        onReducedMotion = { scope.launch { store.setReducedMotion(it) } },
                                        streak = streak,
                                        onWakeLog = { go(Route.WakeLog) },
                                        bedtimeOn = bedtimeOn,
                                        sleepGoal = sleepGoal,
                                        onBedtime = { scope.launch { store.setBedtime(it) } },
                                        onSleepGoal = { scope.launch { store.setSleepGoal(it) } },
                                        onBackup = { exportLauncher.launch("clookie-" + LocalDate.now() + ".json") },
                                        onRestore = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                                        backupStatus = backupStatus,
                                    )
                                }
                            }
                            is Route.Editor -> {
                                val existing = alarms.firstOrNull { it.id == r.alarmId }
                                val start = existing ?: remember(r) { Alarm(id = 0, hour = 7, minute = 0, sound = defaultSound) }
                                AlarmEditorScreen(
                                    initial = start,
                                    isNew = existing == null,
                                    onSave = { a ->
                                        scope.launch { repo.save(a.copy(enabled = if (existing == null) true else a.enabled)) }
                                        pop()
                                    },
                                    onClose = ::pop,
                                    onDelete = {
                                        scope.launch { repo.delete(start.id) }
                                        pop()
                                    },
                                    onPreview = { go(Route.Ringing(it)) },
                                )
                            }
                            is Route.Ringing -> RingingScreen(
                                alarm = r.alarm.copy(snoozeCount = 0),
                                date = LocalDate.now(),
                                onDismissed = ::pop,
                                onSnooze = ::pop,
                            )
                            Route.Themes -> StudioScreen(
                                current = look,
                                currentStyle = clockStyle,
                                customPresets = customPresets,
                                reducedMotion = reducedMotion,
                                alarms = alarms,
                                onApply = { l, st ->
                                    scope.launch {
                                        store.setLook(l)
                                        store.setClockStyle(st)
                                    }
                                },
                                onSave = { l, st ->
                                    scope.launch {
                                        val name = context.getString(R.string.my_preset, customPresets.size + 1)
                                        store.setCustomPresets(listOf(CustomPreset("c" + System.currentTimeMillis(), name, l)) + customPresets)
                                        store.setLook(l)
                                        store.setClockStyle(st)
                                    }
                                },
                                onDeleteCustom = { id -> scope.launch { store.setCustomPresets(customPresets.filter { it.id != id }) } },
                                onReducedMotion = { scope.launch { store.setReducedMotion(it) } },
                                onBack = ::pop,
                            )
                            Route.WakeLog -> WakeLogScreen(wakeLog, LocalDate.now(), onBack = ::pop)
                            is Route.Permissions -> PermissionScreen(
                                rows = rows,
                                onGrant = permissions.grant,
                                onSkip = permissions.skip,
                                onGrantAll = { permissions.grantAll(rows) },
                                onDone = {
                                    scope.launch { store.setOnboarded() }
                                    if (r.firstLaunch) stack[stack.lastIndex] = Route.Main else pop()
                                },
                                onBack = if (r.firstLaunch) null else ::pop,
                            )
                            Route.Reliability -> ReliabilityScreen(
                                rows = rows,
                                onGrant = permissions.grant,
                                onAllPermissions = { go(Route.Permissions(firstLaunch = false)) },
                                onBack = ::pop,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun currentLanguage(): String =
    AppCompatDelegate.getApplicationLocales()[0]?.language?.takeIf { it == "en" } ?: "vi"
