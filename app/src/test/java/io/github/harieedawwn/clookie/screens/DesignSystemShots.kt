package io.github.harieedawwn.clookie.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.ClookieTheme
import io.github.harieedawwn.clookie.design.Preset
import io.github.harieedawwn.clookie.design.Presets
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.icons.ClookieIcon
import io.github.harieedawwn.clookie.design.icons.ClookieIcons
import io.github.harieedawwn.clookie.model.Days
import io.github.harieedawwn.clookie.ui.Eyebrow
import io.github.harieedawwn.clookie.ui.components.BiteAddButton
import io.github.harieedawwn.clookie.ui.components.BiteToggle
import io.github.harieedawwn.clookie.ui.components.ButtonKind
import io.github.harieedawwn.clookie.ui.components.CookieButton
import io.github.harieedawwn.clookie.ui.components.IconCircleButton
import io.github.harieedawwn.clookie.ui.components.PillChip
import io.github.harieedawwn.clookie.ui.components.Txt
import io.github.harieedawwn.clookie.ui.components.WeekdayDots
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Component and type specimen sheets -> design/screens/components_*.png. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "vi-w412dp-h915dp-xhdpi")
class DesignSystemShots {

    @Test
    fun componentSheets() {
        for (p in listOf(Presets.BoSua, Presets.Socola, Presets.GiayOLi, Presets.Dem)) {
            captureRoboImage("../design/screens/components_${p.id}.png") { Sheet(p) }
        }
    }

    @Composable
    private fun Sheet(p: Preset) = ClookieTheme(p, reducedMotion = true) {
        val t = Clookie.type
        Column(
            Modifier.fillMaxWidth().background(Clookie.colors.background).padding(Space.gutter),
            verticalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            Eyebrow("Type · ${p.id}")
            Txt("Ỗ Ữ Ẫ ặ ỹ Ở", t.displayTitle)
            Txt("Thức dậy đúng giờ — Ỗ Ữ Ẫ ặ ỹ Ở", t.title)
            Txt("Bánh quy giòn tan buổi sáng, Ỗ Ữ Ẫ ặ ỹ Ở", t.heading)
            Txt("Đồng hồ báo thức là một chiếc bánh quy. Ỗ Ữ Ẫ ặ ỹ Ở", t.body)
            Txt("NHÃN · Ỗ Ữ Ẫ ặ ỹ Ở", t.eyebrow)
            Eyebrow("Buttons")
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s3)) {
                CookieButton("Lưu", {}, icon = ClookieIcons.Check)
                CookieButton("Kiểm tra", {}, kind = ButtonKind.Secondary)
                CookieButton("Bỏ qua", {}, kind = ButtonKind.Quiet)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s3)) {
                PillChip("+10", false, {})
                PillChip("+20", true, {})
                PillChip("Thử", true, {}, icon = ClookieIcons.Play)
                IconCircleButton(ClookieIcons.Close, "", {})
                IconCircleButton(ClookieIcons.Plus, "", {}, filled = true)
            }
            Eyebrow("Bite toggle · weekday dots")
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s4)) {
                BiteToggle(true, {})
                BiteToggle(false, {})
                BiteAddButton("", {})
                WeekdayDots(Days.Weekdays)
            }
            WeekdayDots(Days(0b1010101), Modifier.width(360.dp), dotSize = 42.dp, onToggle = {})
            Eyebrow("Icons")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s4), verticalArrangement = Arrangement.spacedBy(Space.s3)) {
                listOf(
                    ClookieIcons.Alarm, ClookieIcons.Timer, ClookieIcons.Stopwatch, ClookieIcons.Settings, ClookieIcons.Plus,
                    ClookieIcons.Close, ClookieIcons.Check, ClookieIcons.Back, ClookieIcons.Chevron, ClookieIcons.Snooze,
                    ClookieIcons.Bell, ClookieIcons.Sound, ClookieIcons.Vibrate, ClookieIcons.Repeat, ClookieIcons.Label,
                    ClookieIcons.Shield, ClookieIcons.Camera, ClookieIcons.Qr, ClookieIcons.Math, ClookieIcons.Shake,
                    ClookieIcons.Steps, ClookieIcons.Keyboard, ClookieIcons.Memory, ClookieIcons.Crumbs, ClookieIcons.Play,
                    ClookieIcons.Pause, ClookieIcons.Torch, ClookieIcons.Globe, ClookieIcons.Trash, ClookieIcons.Edit,
                    ClookieIcons.Backspace, ClookieIcons.Download, ClookieIcons.Lock, ClookieIcons.Moon, ClookieIcons.Ramp,
                    ClookieIcons.Skip, ClookieIcons.Mission, ClookieIcons.Cookie, ClookieIcons.CookieCode,
                ).forEach { ClookieIcon(it, null, size = 28.dp) }
            }
        }
    }
}
