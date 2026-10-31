package io.github.harieedawwn.clookie.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import io.github.harieedawwn.clookie.MainActivity
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.data.AlarmRepository
import io.github.harieedawwn.clookie.data.SettingsStore
import io.github.harieedawwn.clookie.design.CornerStyle
import io.github.harieedawwn.clookie.design.Look
import io.github.harieedawwn.clookie.design.Preset
import io.github.harieedawwn.clookie.design.Presets
import io.github.harieedawwn.clookie.design.systemPreset
import io.github.harieedawwn.clookie.design.with
import io.github.harieedawwn.clookie.model.Alarm
import io.github.harieedawwn.clookie.model.AlarmKind
import io.github.harieedawwn.clookie.model.nextUp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Home-screen widget: time + date (TextClock, ticked by the system, never by the app) and the
 * next alarm, in the active preset's colors. Repainted only when alarms or the look change.
 */
class ClookieWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                updateAll(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        suspend fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, ClookieWidget::class.java))
            if (ids.isEmpty()) return
            val look = SettingsStore(context).lookNow()
            val alarms = AlarmRepository.get(context).alarms.first()
            val views = views(context, preset(context, look), alarms)
            manager.updateAppWidget(ids, views)
        }

        fun preset(context: Context, look: Look): Preset {
            val base = Presets.byId(look.base)
            val resolved = if (base.id == Presets.SystemFallback.id && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                systemPreset(context)
            } else base
            return resolved.with(look)
        }

        fun views(context: Context, preset: Preset, alarms: List<Alarm>): RemoteViews {
            val c = preset.colors
            val bg = when (preset.corners) {
                CornerStyle.Soft -> R.drawable.widget_bg_soft
                CornerStyle.Rounded -> R.drawable.widget_bg_rounded
                CornerStyle.Crisp -> R.drawable.widget_bg_crisp
            }
            val next = alarms.filter { it.kind != AlarmKind.Timer }.nextUp(LocalDateTime.now())?.second
            val weekdays = context.resources.getStringArray(R.array.weekday_short)
            val nextText = next?.let { "${weekdays[it.dayOfWeek.value - 1]} · %02d:%02d".format(it.hour, it.minute) }
                ?: context.getString(R.string.no_alarm_on)
            return RemoteViews(context.packageName, R.layout.widget_clock).apply {
                setImageViewResource(R.id.widget_bg, bg)
                setImageViewResource(R.id.widget_shadow, bg)
                setInt(R.id.widget_bg, "setColorFilter", c.surface.toArgb())
                setInt(R.id.widget_shadow, "setColorFilter", c.shadow.toArgb())
                setViewVisibility(R.id.widget_shadow, if (preset.hardShadow) View.VISIBLE else View.INVISIBLE)
                setTextColor(R.id.widget_time, c.ink.toArgb())
                setTextColor(R.id.widget_date, c.inkMuted.toArgb())
                setTextColor(R.id.widget_next, c.ink.toArgb())
                setInt(R.id.widget_mark, "setColorFilter", c.accent.toArgb())
                setInt(R.id.widget_bell, "setColorFilter", c.accent.toArgb())
                setTextViewText(R.id.widget_next, nextText)
                setOnClickPendingIntent(
                    R.id.widget_root,
                    PendingIntent.getActivity(
                        context, 3, Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
            }
        }
    }
}
