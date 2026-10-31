package io.github.harieedawwn.clookie.mission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.design.icons.ClookieIcon
import io.github.harieedawwn.clookie.design.icons.ClookieIcons
import io.github.harieedawwn.clookie.design.rememberHaptics
import io.github.harieedawwn.clookie.model.MissionKeys
import io.github.harieedawwn.clookie.model.MissionTuning
import io.github.harieedawwn.clookie.model.MissionType
import io.github.harieedawwn.clookie.model.ShakeCounter
import io.github.harieedawwn.clookie.model.StepCounter
import io.github.harieedawwn.clookie.ui.components.Txt
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Shake: [MissionKeys.Count] hard shakes; normal handling in bed stays under the threshold. */
object ShakeMission : WakeMission {
    private val t = MissionTuning.Shake
    override val type = MissionType.Shake
    override val settings = listOf(
        MissionSetting(MissionKeys.Count, R.string.setting_shakes, t.counts, t.DefaultCount),
        MissionSetting(
            MissionKeys.Sensitivity, R.string.setting_sensitivity, listOf(1, 2, 3), t.DefaultSensitivity,
            listOf(R.string.sensitivity_low, R.string.level_medium, R.string.sensitivity_high),
        ),
    )

    @Composable
    override fun Content(session: MissionSession, modifier: Modifier) {
        val target = session.spec.param(MissionKeys.Count, t.DefaultCount).coerceAtLeast(1)
        val sensitivity = session.spec.param(MissionKeys.Sensitivity, t.DefaultSensitivity)
        val counter = remember { ShakeCounter(sensitivity) }
        MotionMission(session, target, ClookieIcons.Shake, R.string.shake_hint, Sensor.TYPE_ACCELEROMETER, modifier) { e ->
            counter.onSample(e.timestamp, e.values[0], e.values[1], e.values[2])
        }
    }
}

/** Steps: walk [MissionKeys.Count] steps; step sensor with the activity permission, else the accelerometer. */
object StepsMission : WakeMission {
    private val t = MissionTuning.Steps
    override val type = MissionType.Steps
    override val settings = listOf(MissionSetting(MissionKeys.Count, R.string.setting_steps, t.counts, t.DefaultCount))
    override val permissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listOf(Manifest.permission.ACTIVITY_RECOGNITION) else emptyList()
    override val permissionsOptional = true

    @Composable
    override fun Content(session: MissionSession, modifier: Modifier) {
        val context = LocalContext.current
        val target = session.spec.param(MissionKeys.Count, t.DefaultCount).coerceAtLeast(1)
        val useDetector = remember { stepDetectorUsable(context) }
        val counter = remember { StepCounter() }
        if (useDetector) {
            // The platform's step detector already filters out handling: one event = one step.
            MotionMission(session, target, ClookieIcons.Steps, R.string.steps_hint, Sensor.TYPE_STEP_DETECTOR, modifier) { 1 }
        } else {
            MotionMission(session, target, ClookieIcons.Steps, R.string.steps_hint, Sensor.TYPE_ACCELEROMETER, modifier) { e ->
                counter.onSample(e.timestamp, e.values[0], e.values[1], e.values[2])
            }
        }
    }

    private fun stepDetectorUsable(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        val sm = context.getSystemService(SensorManager::class.java)
        return granted && sm?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null
    }
}

/** Problems when a motion mission has no sensor to read. */
private const val NoSensorProblems = 5

/**
 * Shared host of sensor missions: listens to [sensorType] while shown and until done, turns each
 * event into counts with [onEvent], and shows the progress dial. No sensor = math, never a trap.
 */
@Composable
private fun MotionMission(
    session: MissionSession,
    target: Int,
    icon: ImageVector,
    hint: Int,
    sensorType: Int,
    modifier: Modifier,
    onEvent: (SensorEvent) -> Int,
) {
    val context = LocalContext.current
    val demo = LocalMissionDemo.current
    val haptics = rememberHaptics()
    val reduced = Clookie.motion.reduced
    val scope = rememberCoroutineScope()
    val jolt = remember { Animatable(0f) }
    var count by rememberSaveable { mutableIntStateOf(if (demo) target * 2 / 5 else 0) }
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val sensor = remember { sensorManager?.getDefaultSensor(sensorType) }
    if (sensor == null && !demo) {
        MathPad(session, 3, NoSensorProblems, stringResource(R.string.no_sensor), modifier)
        return
    }
    val event by rememberUpdatedState(onEvent)
    if (sensor != null && !demo) {
        DisposableEffect(sensor) {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) {
                    if (count >= target) return
                    val added = event(e)
                    if (added <= 0) return
                    count = (count + added).coerceAtMost(target)
                    session.interact()
                    if (count >= target) {
                        sensorManager?.unregisterListener(this)
                        haptics.confirm()
                        session.complete()
                    } else {
                        if (count % 10 == 0) haptics.strongTick() else haptics.tick()
                        if (!reduced) scope.launch {
                            jolt.snapTo(1f)
                            jolt.animateTo(0f, tween(MissionTuning.Shake.JoltMs.toInt(), easing = LinearEasing))
                        }
                    }
                }

                override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
            }
            sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            onDispose { sensorManager?.unregisterListener(listener) }
        }
    }
    CountDial(count, target, icon, stringResource(hint), { jolt.value }, modifier)
}

/**
 * Progress dial: a ring of chips filling up and a cookie with a bite in the middle showing the
 * count left; [jolt] (1 -> 0 after each count) wiggles the cookie in the draw phase.
 */
@Composable
private fun CountDial(count: Int, target: Int, icon: ImageVector, hint: String, jolt: () -> Float, modifier: Modifier) {
    val c = Clookie.colors
    val density = LocalDensity.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight - 64.dp, 300.dp).coerceAtLeast(150.dp)
        val joltPx = with(density) { MissionTuning.Shake.JoltDp.dp.toPx() }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(side), contentAlignment = Alignment.Center) {
                val stroke = with(density) { Stroke.regular.toPx() }
                Canvas(Modifier.fillMaxSize()) {
                    drawChipRing(count.toFloat() / target, stroke, c.onRinging)
                    val j = jolt()
                    translate(left = sin(j * PI.toFloat() * 3f) * joltPx * j) { drawCookie(size.minDimension * 0.3f, c.onRinging, c.ringing) }
                }
                // Count left, sized to the cookie (a number, not a time: TimeDisplay is for times).
                val numeral = with(density) { (side * 0.2f).toSp() }
                Txt(
                    "${target - count}", Clookie.type.displayTitle.copy(fontSize = numeral, lineHeight = numeral * 1.4f), color = c.ringing, maxLines = 1,
                    modifier = Modifier.graphicsLayer {
                        val j = jolt()
                        translationX = sin(j * PI.toFloat() * 3f) * joltPx * j
                    },
                )
            }
            Spacer(Modifier.height(Space.s3))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ClookieIcon(icon, null, tint = c.onRinging, size = 22.dp)
                Spacer(Modifier.size(Space.s2))
                Txt(hint, Clookie.type.heading, color = c.onRinging, maxLines = 1)
            }
        }
    }
}

private const val RingChips = 24

/** Chips around the dial: filled up to [progress], outlined after. */
private fun DrawScope.drawChipRing(progress: Float, stroke: Float, ink: Color) {
    val r = size.minDimension / 2f
    val chip = r * 0.075f
    val ring = r - chip - stroke
    val filled = (progress * RingChips).toInt()
    for (i in 0 until RingChips) {
        val a = -PI / 2 + i * 2 * PI / RingChips
        val o = Offset(center.x + ring * cos(a).toFloat(), center.y + ring * sin(a).toFloat())
        // Chips are slightly irregular: every other one a touch smaller.
        val cr = if (i % 2 == 0) chip else chip * 0.8f
        if (i < filled) drawCircle(ink, cr, o) else drawCircle(ink, cr, o, style = DrawStroke(stroke * 0.75f))
    }
}

/** Flat cookie of radius [r] with a bite at the top right and two chips. */
internal fun DrawScope.drawCookie(r: Float, cookie: Color, background: Color) {
    drawCircle(cookie, r, center)
    drawCircle(background, r * 0.36f, Offset(center.x + r * 0.78f, center.y - r * 0.68f))
    drawCircle(background, r * 0.09f, Offset(center.x - r * 0.62f, center.y + r * 0.42f))
    drawCircle(background, r * 0.07f, Offset(center.x + r * 0.5f, center.y + r * 0.62f))
}
