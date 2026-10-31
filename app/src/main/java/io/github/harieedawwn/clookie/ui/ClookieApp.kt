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
import io.github.harieedawwn.clookie.ui.screens.ComingSoonScreen
import io.github.harieedawwn.clookie.ui.screens.HomeScreen
import io.github.harieedawwn.clookie.ui.screens.MainScaffold
import io.github.harieedawwn.clookie.ui.screens.PermissionScreen
import io.github.harieedawwn.clookie.ui.screens.ReliabilityScreen
import io.github.harieedawwn.clookie.ui.screens.RingingScreen
import io.github.harieedawwn.clookie.ui.screens.SettingsScreen
import io.github.harieedawwn.clookie.ui.screens.ThemePickerScreen
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface Route {
    data object Main : Route
    data class Editor(val alarmId: Long?) : Route
    data class Ringing(val alarm: Alarm) : Route
    data object Themes : Route
    data class Permissions(val firstLaunch: Boolean) : Route
    data object Reliability : Route
}

/** App shell: Room-backed alarms, first-launch permission screen, Độ tin cậy, updater. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ClookieApp(store: SettingsStore, repo: AlarmRepository, window: Window?, initialPreset: String?, onboarded: Boolean) {
    val presetId by store.presetId.collectAsState(initial = initialPreset)
    val preset = Presets.byId(presetId)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val alarms by repo.alarms.collectAsState(initial = emptyList())
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

    ClookieTheme(preset) {
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
                                    )
                                    NavDest.Timer, NavDest.Stopwatch -> ComingSoonScreen(tab)
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
                            Route.Themes -> ThemePickerScreen(
                                current = preset,
                                alarms = alarms,
                                onApply = { scope.launch { store.setPreset(it.id) } },
                                onBack = ::pop,
                            )
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
