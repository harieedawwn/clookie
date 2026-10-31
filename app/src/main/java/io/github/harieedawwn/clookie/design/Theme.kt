package io.github.harieedawwn.clookie.design

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

val LocalClookieColors = staticCompositionLocalOf { Presets.BoSua.colors }
val LocalClookieType = staticCompositionLocalOf { ClookieType() }
val LocalClookieShapes = staticCompositionLocalOf { ClookieShapes.of(CornerStyle.Rounded) }
val LocalClookieStyle = staticCompositionLocalOf { ClookieStyle(hardShadow = true) }
val LocalClookieMotion = staticCompositionLocalOf { ClookieMotion() }

/** Accessors: `Clookie.colors.ink`, `Clookie.type.body`, ... */
object Clookie {
    val colors: ClookieColors @Composable @ReadOnlyComposable get() = LocalClookieColors.current
    val type: ClookieType @Composable @ReadOnlyComposable get() = LocalClookieType.current
    val shapes: ClookieShapes @Composable @ReadOnlyComposable get() = LocalClookieShapes.current
    val style: ClookieStyle @Composable @ReadOnlyComposable get() = LocalClookieStyle.current
    val motion: ClookieMotion @Composable @ReadOnlyComposable get() = LocalClookieMotion.current
}

/** Resolves "Màu hệ thống" to live Material You tones on Android 12+. */
@Composable
fun resolvePreset(preset: Preset): Preset {
    if (preset.id != Presets.SystemFallback.id || Build.VERSION.SDK_INT < Build.VERSION_CODES.S || LocalInspectionMode.current) {
        return preset
    }
    val context = LocalContext.current
    return remember(context) { systemPreset(context) }
}

@Composable
fun ClookieTheme(
    preset: Preset = Presets.BoSua,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val resolved = resolvePreset(preset)
    val colors = resolved.colors
    val context = LocalContext.current
    val systemNoAnim = remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    val motion = ClookieMotion(if (reducedMotion || systemNoAnim) MotionLevel.Reduced else MotionLevel.Full)
    // Material is only a fallback for platform pieces (text selection, dialogs); Clookie
    // components never read MaterialTheme.
    val scheme = if (colors.isDark) {
        darkColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent, background = colors.background,
            onBackground = colors.ink, surface = colors.surface, onSurface = colors.ink, outline = colors.outline,
        )
    } else {
        lightColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent, background = colors.background,
            onBackground = colors.ink, surface = colors.surface, onSurface = colors.ink, outline = colors.outline,
        )
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalClookieColors provides colors,
            LocalClookieShapes provides ClookieShapes.of(resolved.corners),
            LocalClookieStyle provides ClookieStyle(hardShadow = resolved.hardShadow),
            LocalClookieMotion provides motion,
            LocalIndication provides FlatIndication,
            LocalTextSelectionColors provides TextSelectionColors(colors.accent, colors.accentSoft),
            content = content,
        )
    }
}
