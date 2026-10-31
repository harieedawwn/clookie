package io.github.harieedawwn.clookie.mission

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import io.github.harieedawwn.clookie.model.MissionSpec
import io.github.harieedawwn.clookie.model.MissionType

/**
 * Wake mission plug-in contract (design/DESIGN.md "Missions"). A mission is one object
 * implementing [WakeMission] plus one entry in [MissionRegistry]; the alarm engine, the
 * ringing host and the editor never change when a mission is added.
 */
interface WakeMission {
    val type: MissionType

    /** Difficulty settings, rendered by the editor's mission sheet; values land in [MissionSpec.params]. */
    val settings: List<MissionSetting>

    /** The ringing notification may offer "Tắt" without opening the mission (trivial missions only). */
    val quickDismiss: Boolean get() = false

    /** Runtime permissions the mission needs (e.g. CAMERA); asked inline before it is added. */
    val permissions: List<String> get() = emptyList()

    /** The mission still works without [permissions] (Steps: accelerometer instead of the step sensor). */
    val permissionsOptional: Boolean get() = false

    /**
     * Mission UI, hosted in the ringing screen's mission slot on the `ringing` color.
     * Call [MissionSession.interact] on every user interaction and [MissionSession.complete] once.
     */
    @Composable
    fun Content(session: MissionSession, modifier: Modifier)

    /**
     * Optional editor content under the setting chips, for choices that are not fixed numbers
     * (e.g. which of the user's codes to scan, and managing those codes).
     */
    @Composable
    fun EditorExtras(spec: MissionSpec, onChange: (MissionSpec) -> Unit) {}
}

/** What a running mission can see and do. */
interface MissionSession {
    val spec: MissionSpec

    /** "Thử" preview from the editor: no sound, completing returns to the editor. */
    val preview: Boolean

    /** User is working on the mission: alarm volume ducks to 30% until 20 s of no interaction. */
    fun interact()

    /** Mission solved: the host moves to the next mission in the chain or dismisses the alarm. */
    fun complete()
}

/** One difficulty setting: a labeled choice among integer [values] shown as chips. */
@Immutable
data class MissionSetting(
    val key: String,
    @StringRes val label: Int,
    val values: List<Int>,
    val default: Int,
    /** Optional per-value label resources (same size as [values]); numbers are shown otherwise. */
    val valueLabels: List<Int>? = null,
)

/**
 * Screenshots and previews: no sensors, camera or game loop; missions draw a fixed mid-game
 * frame instead of falling back to math.
 */
val LocalMissionDemo = staticCompositionLocalOf { false }

/** All missions this build ships. Stages 3-4 add one line each. */
object MissionRegistry {
    val all: List<WakeMission> = listOf(
        ClassicMission,
        CookieCodeMission,
        QrMission,
        MathMission,
        ShakeMission,
        StepsMission,
        TypingMission,
        MemoryMission,
        CrumbsMission,
    )

    fun of(type: MissionType): WakeMission? = all.firstOrNull { it.type == type }

    /** Never trap the user: an unknown or unavailable mission runs as Classic. */
    fun resolve(spec: MissionSpec): Pair<WakeMission, MissionSpec> =
        of(spec.type)?.let { it to spec } ?: (ClassicMission to defaultSpec(ClassicMission))

    fun defaultSpec(mission: WakeMission) = MissionSpec(mission.type, mission.settings.associate { it.key to it.default })

    /** True when every mission of the chain allows dismissing from the notification. */
    fun quickDismiss(chain: List<MissionSpec>): Boolean = chain.all { resolve(it).first.quickDismiss }
}
