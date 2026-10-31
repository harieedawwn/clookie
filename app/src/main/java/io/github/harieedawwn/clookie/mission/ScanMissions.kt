package io.github.harieedawwn.clookie.mission

import android.Manifest
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.data.CodeRepository
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.design.icons.ClookieIcons
import io.github.harieedawwn.clookie.design.rememberHaptics
import io.github.harieedawwn.clookie.model.CookieCode
import io.github.harieedawwn.clookie.model.MathProblem
import io.github.harieedawwn.clookie.model.MissionSpec
import io.github.harieedawwn.clookie.model.MissionTuning
import io.github.harieedawwn.clookie.model.MissionType
import io.github.harieedawwn.clookie.model.ScanCode
import io.github.harieedawwn.clookie.model.ScanGate
import io.github.harieedawwn.clookie.scan.BarcodeScanner
import io.github.harieedawwn.clookie.scan.CameraFeed
import io.github.harieedawwn.clookie.scan.CookieScanner
import io.github.harieedawwn.clookie.scan.Scanner
import io.github.harieedawwn.clookie.scan.hasCamera
import io.github.harieedawwn.clookie.ui.components.CookieGuide
import io.github.harieedawwn.clookie.ui.components.RingingChip
import io.github.harieedawwn.clookie.ui.components.RingingNumberPad
import io.github.harieedawwn.clookie.ui.components.ScanWindow
import io.github.harieedawwn.clookie.ui.components.Txt
import kotlin.random.Random

/** The spec's code id (0 = any of the user's codes). */
const val KeyCode = "code"

/** Codes for previews and screenshots; production reads the database. */
class CodeSamples(val cookie: List<CookieCode>, val scan: List<ScanCode>)

val LocalCodeSamples = staticCompositionLocalOf<CodeSamples?> { null }

@Composable
internal fun rememberCookieCodes(): List<CookieCode>? {
    val samples = LocalCodeSamples.current
    if (samples != null) return samples.cookie
    if (LocalInspectionMode.current) return emptyList()
    val repo = CodeRepository.get(LocalContext.current)
    val codes by produceState<List<CookieCode>?>(null, repo) { repo.cookieCodes.collect { value = it } }
    return codes
}

@Composable
internal fun rememberScanCodes(): List<ScanCode>? {
    val samples = LocalCodeSamples.current
    if (samples != null) return samples.scan
    if (LocalInspectionMode.current) return emptyList()
    val repo = CodeRepository.get(LocalContext.current)
    val codes by produceState<List<ScanCode>?>(null, repo) { repo.scanCodes.collect { value = it } }
    return codes
}

/** The codes an alarm accepts: the chosen one, or all of them when "any" or the choice is gone. */
internal fun <T> chosen(all: List<T>, id: Long, idOf: (T) -> Long): List<T> = all.filter { idOf(it) == id }.ifEmpty { all }

/**
 * Cookie Code: the camera opens at once; only the user's hand-drawn code stops the alarm
 * (3 consecutive frames). Torch, auto-torch, "Mất mã?" backup, math when no camera.
 */
object CookieCodeMission : WakeMission {
    override val type = MissionType.CookieCode
    override val settings = emptyList<MissionSetting>()
    override val permissions = listOf(Manifest.permission.CAMERA)

    @Composable
    override fun Content(session: MissionSession, modifier: Modifier) {
        val codes = rememberCookieCodes()
        val targets = codes?.let { chosen(it, session.spec.param(KeyCode, 0).toLong()) { c -> c.id } }
        val scanner = remember { CookieScanner(emptyList()) }
        LaunchedEffect(targets) { if (targets != null) scanner.setTargets(targets) }
        ScanMission(session, scanner, targets?.isNotEmpty(), modifier) {
            targets?.firstOrNull()?.let { CookieGuide(it.pattern, step = 99, progress = { 1f }, Modifier.fillMaxSize()) }
        }
    }

    @Composable
    override fun EditorExtras(spec: MissionSpec, onChange: (MissionSpec) -> Unit) = CookieCodeEditor(spec, onChange)
}

/** QR / barcode: scan a registered code (shampoo, toothpaste, a printed QR). Offline (ZXing). */
object QrMission : WakeMission {
    override val type = MissionType.Qr
    override val settings = emptyList<MissionSetting>()
    override val permissions = listOf(Manifest.permission.CAMERA)

    @Composable
    override fun Content(session: MissionSession, modifier: Modifier) {
        val codes = rememberScanCodes()
        val targets = codes?.let { chosen(it, session.spec.param(KeyCode, 0).toLong()) { c -> c.id } }
        val scanner = remember { BarcodeScanner(emptyList()) }
        LaunchedEffect(targets) { if (targets != null) scanner.setTargets(targets) }
        ScanMission(session, scanner, targets?.isNotEmpty(), modifier) {}
    }

    @Composable
    override fun EditorExtras(spec: MissionSpec, onChange: (MissionSpec) -> Unit) = QrEditor(spec, onChange)
}

private enum class ScanMode { Scan, Lost, NoCamera }

/** Problems in the "Mất mã?" backup: deliberately long. */
const val LostCodeProblems = 15

/** Problems when the camera cannot be used (no permission, error, no codes). */
const val NoCameraProblems = 5

/**
 * Shared ringing UI of scan missions. [hasCodes] is null while the codes load. Falls back to
 * math when the camera is missing or fails, or there is nothing to scan; never traps the user.
 */
@Composable
private fun ScanMission(
    session: MissionSession,
    scanner: Scanner,
    hasCodes: Boolean?,
    modifier: Modifier,
    placeholder: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Previews and screenshots: no camera, the code on paper stands in for it.
    val previewing = LocalInspectionMode.current || LocalCodeSamples.current != null
    val c = Clookie.colors
    val haptics = rememberHaptics()
    val cameraOk = remember { previewing || hasCamera(context) }
    var mode by rememberSaveable { mutableStateOf(if (cameraOk) ScanMode.Scan else ScanMode.NoCamera) }
    LaunchedEffect(hasCodes) { if (hasCodes == false && mode == ScanMode.Scan) mode = ScanMode.NoCamera }
    val accepted = scanner.accepted
    LaunchedEffect(accepted) {
        if (accepted != ScanGate.NoMatch) {
            haptics.confirm()
            session.complete()
        }
    }
    when (mode) {
        ScanMode.Lost -> MathPad(session, 3, LostCodeProblems, null, modifier)
        ScanMode.NoCamera -> MathPad(
            session, 3, NoCameraProblems,
            stringResource(if (hasCodes == false) R.string.scan_no_codes else R.string.scan_no_camera), modifier,
        )
        ScanMode.Scan -> BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
          // The window fits what the ringing screen leaves, above the controls row.
          val side = minOf(maxWidth, maxHeight - 64.dp, 320.dp).coerceAtLeast(160.dp)
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            var torch by rememberSaveable { mutableStateOf(false) }
            var torchTouched by rememberSaveable { mutableStateOf(false) }
            var torchAvailable by remember { mutableStateOf(true) }
            LaunchedEffect(scanner.dark) {
                if (scanner.dark && !torchTouched && torchAvailable) torch = true
            }
            // Aiming at a code counts as working on the mission: the alarm ducks.
            LaunchedEffect(scanner.streak, scanner.wrong) { if (scanner.streak > 0 || scanner.wrong) session.interact() }
            ScanWindow(
                streak = scanner.streak,
                required = scanner.required,
                wrong = if (scanner.wrong) stringResource(R.string.scan_wrong) else null,
                ink = c.onRinging,
                fill = c.ringing,
                modifier = Modifier.size(side),
            ) {
                if (previewing) {
                    placeholder()
                } else if (hasCodes == true) {
                    CameraFeed(
                        scanner, torch,
                        onError = { mode = ScanMode.NoCamera },
                        onTorchAvailable = { torchAvailable = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(Modifier.height(Space.s3))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                if (torchAvailable) {
                    RingingChip(
                        stringResource(R.string.scan_torch), ClookieIcons.Torch,
                        onClick = {
                            torch = !torch
                            torchTouched = true
                            session.interact()
                        },
                        selected = torch,
                    )
                }
                RingingChip(stringResource(R.string.scan_lost), null, onClick = {
                    session.interact()
                    mode = ScanMode.Lost
                })
            }
          }
        }
    }
}

/**
 * [count] problems of [level] typed on a number pad; a wrong answer clears and buzzes. The Math
 * mission and every fallback ("Mất mã?", no camera, no sensor) use it. [reason] says why it replaced a mission.
 */
@Composable
internal fun MathPad(session: MissionSession, level: Int, count: Int, reason: String?, modifier: Modifier) {
    val c = Clookie.colors
    val haptics = rememberHaptics()
    val seed = rememberSaveable { Random.nextLong() }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var typed by rememberSaveable { mutableStateOf("") }
    val problem = remember(index) { MathProblem.generate(level, Random(seed + index)) }
    BoxWithConstraints(modifier) {
    // Keys shrink on short screens so the whole pad stays above the snooze button.
    val key = ((maxHeight - (if (reason != null) 150.dp else 126.dp)) / 4 - Space.s2).coerceIn(40.dp, 52.dp)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (reason != null) {
            Txt(reason, Clookie.type.label, color = c.onRinging, align = TextAlign.Center)
            Spacer(Modifier.height(Space.s1))
        }
        Txt(stringResource(R.string.backup_progress, index + 1, count).uppercase(), Clookie.type.eyebrow, color = c.onRinging)
        Spacer(Modifier.height(Space.s2))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s3)) {
            Txt("${problem.text} =", Clookie.type.displaySmall, color = c.onRinging, maxLines = 1)
            Box(
                Modifier
                    .widthIn(min = 88.dp)
                    .heightIn(min = 52.dp)
                    .border(Stroke.regular, c.onRinging, Clookie.shapes.small)
                    .padding(horizontal = Space.s3),
                contentAlignment = Alignment.Center,
            ) {
                Txt(typed.ifEmpty { "?" }, Clookie.type.displaySmall, color = c.onRinging, maxLines = 1)
            }
        }
        Spacer(Modifier.height(Space.s3))
        RingingNumberPad(
            onDigit = { d ->
                session.interact()
                if (typed.length < MissionTuning.Math.MaxDigits) typed += d
            },
            onBackspace = {
                session.interact()
                typed = typed.dropLast(1)
            },
            onOk = {
                session.interact()
                if (typed.toIntOrNull() == problem.answer) {
                    haptics.confirm()
                    typed = ""
                    if (index + 1 >= count) session.complete() else index++
                } else {
                    haptics.reject()
                    typed = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            backspaceLabel = stringResource(R.string.cd_backspace),
            okLabel = stringResource(R.string.action_check),
            keyHeight = key,
        )
    }
    }
}
