package io.github.harieedawwn.clookie.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.design.icons.ClookieIcons
import io.github.harieedawwn.clookie.model.Countdown
import io.github.harieedawwn.clookie.model.MissionType
import io.github.harieedawwn.clookie.model.SleeperProfile
import io.github.harieedawwn.clookie.model.VibrationPattern
import io.github.harieedawwn.clookie.reliability.PermItem

val MissionType.icon: ImageVector
    get() = when (this) {
        MissionType.Classic -> ClookieIcons.Check
        MissionType.CookieCode -> ClookieIcons.CookieCode
        MissionType.Qr -> ClookieIcons.Qr
        MissionType.Math -> ClookieIcons.Math
        MissionType.Shake -> ClookieIcons.Shake
        MissionType.Steps -> ClookieIcons.Steps
        MissionType.Typing -> ClookieIcons.Keyboard
        MissionType.Memory -> ClookieIcons.Memory
        MissionType.Crumbs -> ClookieIcons.Crumbs
    }

@get:StringRes
val MissionType.nameRes: Int
    get() = when (this) {
        MissionType.Classic -> R.string.mission_classic
        MissionType.CookieCode -> R.string.mission_cookie_code
        MissionType.Qr -> R.string.mission_qr
        MissionType.Math -> R.string.mission_math
        MissionType.Shake -> R.string.mission_shake
        MissionType.Steps -> R.string.mission_steps
        MissionType.Typing -> R.string.mission_typing
        MissionType.Memory -> R.string.mission_memory
        MissionType.Crumbs -> R.string.mission_crumbs
    }

@get:StringRes
val SleeperProfile.nameRes: Int
    get() = when (this) {
        SleeperProfile.Light -> R.string.profile_light
        SleeperProfile.Medium -> R.string.profile_medium
        SleeperProfile.Deep -> R.string.profile_deep
    }

@get:StringRes
val VibrationPattern.nameRes: Int
    get() = when (this) {
        VibrationPattern.Off -> R.string.off
        VibrationPattern.Heartbeat -> R.string.vibration_heartbeat
        VibrationPattern.Steady -> R.string.vibration_steady
        VibrationPattern.Crescendo -> R.string.vibration_crescendo
    }

val PermItem.icon: ImageVector
    get() = when (this) {
        PermItem.Notifications -> ClookieIcons.Bell
        PermItem.ExactAlarm -> ClookieIcons.Alarm
        PermItem.FullScreen -> ClookieIcons.Fullscreen
        PermItem.Battery -> ClookieIcons.Battery
        PermItem.Autostart -> ClookieIcons.Power
        PermItem.Camera -> ClookieIcons.Camera
        PermItem.Activity -> ClookieIcons.Steps
        PermItem.InstallUpdates -> ClookieIcons.Download
        PermItem.Overlay -> ClookieIcons.Layers
    }

@get:StringRes
val PermItem.nameRes: Int
    get() = when (this) {
        PermItem.Notifications -> R.string.perm_notifications
        PermItem.ExactAlarm -> R.string.perm_exact
        PermItem.FullScreen -> R.string.perm_fullscreen
        PermItem.Battery -> R.string.perm_battery
        PermItem.Autostart -> R.string.perm_autostart
        PermItem.Camera -> R.string.perm_camera
        PermItem.Activity -> R.string.perm_activity
        PermItem.InstallUpdates -> R.string.perm_install
        PermItem.Overlay -> R.string.perm_overlay
    }

@Composable
fun countdownText(c: Countdown): String = when {
    c.days > 0 -> stringResource(R.string.countdown_dh, c.days, c.hours)
    c.hours > 0 -> stringResource(R.string.countdown_hm, c.hours, c.minutes)
    else -> stringResource(R.string.countdown_m, c.minutes)
}
