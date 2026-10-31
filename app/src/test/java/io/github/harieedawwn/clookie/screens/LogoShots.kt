package io.github.harieedawwn.clookie.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.design.ClookieTheme
import io.github.harieedawwn.clookie.design.DisplayFamily
import io.github.harieedawwn.clookie.ui.components.Txt
import io.github.harieedawwn.clookie.design.Clookie
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.cos
import kotlin.math.sin

private val Jam = Color(0xFFC2352A)
private val Cream = Color(0xFFF6ECD9)
private val Caramel = Color(0xFFDDA05A)
private val Cocoa = Color(0xFF2B1A10)

/**
 * Logo concepts -> design/logo/. A ("bitten clock") is the chosen mark and is rendered from
 * the real launcher drawables, so the PNG is exactly what ships.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w420dp-h420dp-xhdpi")
class LogoShots {

    @Test
    fun conceptA_bittenClock() = captureRoboImage("../design/logo/concept_a_bitten_clock.png") {
        Board {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                LauncherIcon(176.dp, CircleShape)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LauncherIcon(88.dp, RoundedCornerShape(26.dp))
                    Monochrome(88.dp, Color(0xFFDCE4D5), Color(0xFF223018))
                }
            }
            SmallSizes { s -> LauncherIcon(s, CircleShape) }
        }
    }

    @Test
    fun conceptB_biteFace() = captureRoboImage("../design/logo/concept_b_bite_face.png") {
        Board {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(176.dp).clip(CircleShape).background(Cream)) { BiteFace(Modifier.size(176.dp)) }
                Box(Modifier.size(88.dp).clip(RoundedCornerShape(26.dp)).background(Cocoa)) { BiteFace(Modifier.size(88.dp), mono = Cream) }
            }
            SmallSizes { s -> Box(Modifier.size(s).clip(CircleShape).background(Cream)) { BiteFace(Modifier.size(s)) } }
        }
    }

    @Test
    fun conceptC_wordmark() = captureRoboImage("../design/logo/concept_c_wordmark.png") {
        Board {
            Wordmark(64)
            Wordmark(28)
            SmallSizes { s -> Box(Modifier.size(s).clip(CircleShape).background(Jam), contentAlignment = Alignment.Center) { OoGlyph(s * 0.8f) } }
        }
    }

    @Composable
    private fun Board(content: @Composable () -> Unit) = ClookieTheme {
        Column(
            Modifier.background(Color(0xFFEFE6D6)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) { content() }
    }

    /** 48 px and 24 px real-pixel renditions (xhdpi: 1 dp = 2 px). */
    @Composable
    private fun SmallSizes(item: @Composable (Dp) -> Unit) {
        val px = LocalDensity.current.density
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            item((48 / px).dp)
            item((24 / px).dp)
            Txt("48 px · 24 px", Clookie.type.label)
        }
    }

    /** Launchers show the middle 72 of the 108-unit adaptive layers, so draw at 1.5x and clip. */
    @Composable
    private fun LauncherIcon(size: Dp, shape: androidx.compose.ui.graphics.Shape) {
        Box(Modifier.size(size).clip(shape).background(Jam), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(size * 1.5f))
        }
    }

    @Composable
    private fun Monochrome(size: Dp, bg: Color, fg: Color) {
        Box(Modifier.size(size).clip(RoundedCornerShape(26.dp)).background(bg), contentAlignment = Alignment.Center) {
            Image(
                painterResource(R.drawable.ic_launcher_monochrome), null, Modifier.requiredSize(size * 1.5f),
                colorFilter = ColorFilter.tint(fg),
            )
        }
    }

    /** Concept B: a clock face whose 1-to-3 o'clock sector was bitten away; chips are the hour marks. */
    @Composable
    private fun BiteFace(modifier: Modifier, mono: Color? = null) = Canvas(modifier) {
        val c = center
        val r = size.minDimension * 0.34f
        val body = Path().apply { addOval(Rect(c, r)) }
        val biteC = Offset(c.x + r * 0.95f, c.y - r * 0.6f)
        val bite = Path().apply { addOval(Rect(biteC, r * 0.62f)) }
        val face = Path().apply { op(body, bite, PathOperation.Difference) }
        drawPath(face, mono ?: Caramel)
        val chip = Cocoa
        for (h in 0 until 12) {
            val a = Math.toRadians(h * 30.0)
            val p = Offset(c.x + r * 0.78f * sin(a).toFloat(), c.y - r * 0.78f * cos(a).toFloat())
            if ((p - biteC).getDistance() > r * 0.72f) drawCircle(chip, r * 0.075f, p)
        }
        drawLine(chip, c, Offset(c.x + r * 0.45f, c.y - r * 0.28f), r * 0.12f, StrokeCap.Round)
        drawLine(chip, c, Offset(c.x - r * 0.05f, c.y + r * 0.42f), r * 0.12f, StrokeCap.Round)
        drawCircle(chip, r * 0.1f, c)
    }

    /** Concept C: "cl", a cookie "o", a clock "o", "kie". */
    @Composable
    private fun Wordmark(sizeSp: Int) {
        val style = Clookie.type.displayTitle.copy(fontFamily = DisplayFamily, fontSize = sizeSp.sp, lineHeight = (sizeSp * 1.2).sp)
        val o = with(LocalDensity.current) { (sizeSp * 0.56f).sp.toDp() }
        Row(verticalAlignment = Alignment.Bottom) {
            Txt("cl", style, color = Cocoa)
            OoGlyph(o * 2.1f, Modifier.padding(bottom = o * 0.18f))
            Txt("kie", style, color = Cocoa)
        }
    }

    @Composable
    private fun OoGlyph(width: Dp, modifier: Modifier = Modifier) = Canvas(modifier.size(width, width / 2f)) {
        val r = size.height / 2f * 0.92f
        val c1 = Offset(size.width * 0.25f, size.height / 2f)
        val c2 = Offset(size.width * 0.75f, size.height / 2f)
        drawCircle(Caramel, r, c1)
        drawCircle(Cocoa, r * 0.14f, c1 + Offset(-r * 0.35f, -r * 0.2f))
        drawCircle(Cocoa, r * 0.12f, c1 + Offset(r * 0.3f, r * 0.3f))
        drawCircle(Cocoa, r * 0.1f, c1 + Offset(-r * 0.2f, r * 0.45f))
        drawCircle(Cocoa, r * 0.86f, c2, style = Stroke(r * 0.28f))
        drawLine(Cocoa, c2, c2 + Offset(0f, -r * 0.55f), r * 0.2f, StrokeCap.Round)
        drawLine(Cocoa, c2, c2 + Offset(r * 0.4f, 0f), r * 0.2f, StrokeCap.Round)
    }
}

