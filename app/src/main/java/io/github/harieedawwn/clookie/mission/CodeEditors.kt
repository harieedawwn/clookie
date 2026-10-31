package io.github.harieedawwn.clookie.mission

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.data.CodeRepository
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.design.icons.ClookieIcon
import io.github.harieedawwn.clookie.design.icons.ClookieIcons
import io.github.harieedawwn.clookie.design.rememberHaptics
import io.github.harieedawwn.clookie.model.CookieCode
import io.github.harieedawwn.clookie.model.CookiePattern
import io.github.harieedawwn.clookie.model.MissionSpec
import io.github.harieedawwn.clookie.model.ScanCode
import io.github.harieedawwn.clookie.model.ScanGate
import io.github.harieedawwn.clookie.scan.BarcodeScanner
import io.github.harieedawwn.clookie.scan.CameraFeed
import io.github.harieedawwn.clookie.scan.CookieScanner
import io.github.harieedawwn.clookie.scan.Decoded
import io.github.harieedawwn.clookie.ui.Eyebrow
import io.github.harieedawwn.clookie.ui.TopBar
import io.github.harieedawwn.clookie.ui.components.ButtonKind
import io.github.harieedawwn.clookie.ui.components.ClookieOverlay
import io.github.harieedawwn.clookie.ui.components.CookieButton
import io.github.harieedawwn.clookie.ui.components.CookieCodeIcon
import io.github.harieedawwn.clookie.ui.components.CookieGuide
import io.github.harieedawwn.clookie.ui.components.IconCircleButton
import io.github.harieedawwn.clookie.ui.components.PillChip
import io.github.harieedawwn.clookie.ui.components.ScanWindow
import io.github.harieedawwn.clookie.ui.components.Txt
import io.github.harieedawwn.clookie.ui.components.guideSteps
import io.github.harieedawwn.clookie.ui.rememberPermissionRequest
import io.github.harieedawwn.clookie.ui.screenBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun MissionSpec.withCode(id: Long) = copy(params = params + (KeyCode to id.toInt()))

/** "Code to scan" chips: any, each code, and the button that opens the code screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CodeChips(codes: List<Pair<Long, String>>, selected: Long, onSelect: (Long) -> Unit, manage: String, onManage: () -> Unit) {
    Spacer(Modifier.height(Space.s3))
    Txt(stringResource(R.string.code_to_scan), Clookie.type.label, color = Clookie.colors.inkMuted)
    Spacer(Modifier.height(Space.s2))
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.s2),
        verticalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        if (codes.isNotEmpty()) PillChip(stringResource(R.string.code_any), selected = selected == 0L, onClick = { onSelect(0) })
        codes.forEach { (id, name) -> PillChip(name, selected = selected == id, onClick = { onSelect(id) }) }
        PillChip(manage, selected = false, icon = if (codes.isEmpty()) ClookieIcons.Plus else ClookieIcons.Edit, onClick = onManage)
    }
}

@Composable
private fun NameField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = Clookie.colors
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.take(24)) },
        singleLine = true,
        textStyle = Clookie.type.heading.merge(color = c.ink),
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = modifier,
        decorationBox = { inner ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(c.surface, Clookie.shapes.medium)
                    .border(Stroke.regular, c.outline, Clookie.shapes.medium)
                    .padding(horizontal = Space.s4, vertical = Space.s3),
            ) {
                Txt(stringResource(R.string.code_name), Clookie.type.caption, color = c.inkMuted)
                inner()
            }
        },
    )
}

/** Code list row: icon, name, detail, trailing actions; delete needs a second tap. */
@Composable
private fun CodeRow(
    icon: @Composable () -> Unit,
    name: String,
    detail: String?,
    onDelete: () -> Unit,
    extra: (@Composable () -> Unit)? = null,
) {
    val c = Clookie.colors
    var confirm by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.surface, Clookie.shapes.medium)
            .border(Stroke.thin, c.outline, Clookie.shapes.medium)
            .padding(Space.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(Space.s3))
        Column(Modifier.weight(1f)) {
            Txt(name, Clookie.type.heading, maxLines = 1)
            if (detail != null) Txt(detail, Clookie.type.label, color = c.inkMuted, maxLines = 1)
        }
        extra?.invoke()
        Spacer(Modifier.width(Space.s2))
        if (confirm) {
            PillChip(stringResource(R.string.action_delete), selected = true, onClick = onDelete)
        } else {
            IconCircleButton(ClookieIcons.Trash, stringResource(R.string.action_delete), { confirm = true }, size = 44.dp)
        }
    }
}

// ---------------------------------------------------------------- Cookie Code

@Composable
internal fun CookieCodeEditor(spec: MissionSpec, onChange: (MissionSpec) -> Unit) {
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    val scope = rememberCoroutineScope()
    val codes = rememberCookieCodes().orEmpty()
    var studio by rememberSaveable { mutableStateOf(false) }
    val id = spec.param(KeyCode, 0).toLong()
    val selected = if (codes.any { it.id == id }) id else 0L
    CodeChips(
        codes.map { it.id to it.name }, selected, { onChange(spec.withCode(it)) },
        stringResource(if (codes.isEmpty()) R.string.code_new else R.string.code_mine), { studio = true },
    )
    if (!studio || inspection) return
    val repo = CodeRepository.get(context)
    var exporting by remember { mutableStateOf<CookieCode?>(null) }
    var exported by remember { mutableLongStateOf(-1L) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val code = exporting
        exporting = null
        if (uri != null && code != null) {
            scope.launch {
                withContext(Dispatchers.IO) { writeCookieCodePng(context, uri, code.pattern) }
                exported = code.id
            }
        }
    }
    ClookieOverlay(onDismiss = { studio = false }) {
        CookieCodeStudio(
            codes = codes,
            onClose = { studio = false },
            onSave = { name, pattern ->
                scope.launch { onChange(spec.withCode(repo.addCookieCode(name, pattern))) }
            },
            onDelete = { scope.launch { repo.deleteCookieCode(it.id) } },
            onExport = {
                exporting = it
                export.launch("Clookie-${it.name}.png")
            },
            exportedId = exported,
        )
    }
}

/**
 * The user's Cookie Codes: list (save image, delete) and the new-code flow: generated pattern,
 * drawing guide (frame then pattern; play/pause, step, "2/5"), "Quét thử", then save.
 */
@Composable
fun CookieCodeStudio(
    codes: List<CookieCode>,
    onClose: () -> Unit,
    onSave: (String, Int) -> Unit,
    onDelete: (CookieCode) -> Unit,
    onExport: (CookieCode) -> Unit,
    modifier: Modifier = Modifier,
    exportedId: Long = -1,
    startNew: Boolean = codes.isEmpty(),
    pattern: Int? = null,
    tested: Boolean = false,
) {
    var creating by rememberSaveable { mutableStateOf(startNew) }
    val c = Clookie.colors
    Column(
        modifier
            .fillMaxSize()
            .screenBackground()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.gutter, vertical = Space.s2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (creating) {
            NewCookieCode(
                others = codes.map { it.pattern },
                defaultName = stringResource(R.string.code_default_name, codes.size + 1),
                onBack = { if (codes.isEmpty()) onClose() else creating = false },
                onSave = { name, p ->
                    onSave(name, p)
                    creating = false
                },
                initialPattern = pattern,
                initialTested = tested,
            )
        } else {
            TopBar(
                stringResource(R.string.code_mine),
                leading = { IconCircleButton(ClookieIcons.Close, stringResource(R.string.action_close), onClose) },
            )
            Spacer(Modifier.height(Space.s4))
            Column(verticalArrangement = Arrangement.spacedBy(Space.s3)) {
                codes.forEach { code ->
                    CodeRow(
                        icon = { CookieCodeIcon(code.pattern, c.ink, size = 44.dp) },
                        name = code.name,
                        detail = if (exportedId == code.id) stringResource(R.string.code_image_saved) else null,
                        onDelete = { onDelete(code) },
                        extra = { IconCircleButton(ClookieIcons.Download, stringResource(R.string.code_save_image), { onExport(code) }, size = 44.dp) },
                    )
                }
            }
            if (codes.isEmpty()) Txt(stringResource(R.string.code_none), Clookie.type.body, color = c.inkMuted)
            Spacer(Modifier.height(Space.s5))
            CookieButton(stringResource(R.string.code_new), { creating = true }, Modifier.fillMaxWidth(), icon = ClookieIcons.Plus)
        }
        Spacer(Modifier.height(Space.s6))
    }
}

@Composable
private fun NewCookieCode(
    others: List<Int>,
    defaultName: String,
    onBack: () -> Unit,
    onSave: (String, Int) -> Unit,
    initialPattern: Int?,
    initialTested: Boolean,
) {
    val c = Clookie.colors
    val haptics = rememberHaptics()
    val inspection = LocalInspectionMode.current
    var pattern by rememberSaveable { mutableIntStateOf(initialPattern ?: CookiePattern.generate(others) ?: -1) }
    var name by rememberSaveable { mutableStateOf(defaultName) }
    var tested by rememberSaveable(pattern) { mutableStateOf(initialTested) }
    var scanning by remember(pattern) { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }
    val askCamera = rememberPermissionRequest { ok ->
        denied = !ok
        scanning = ok
    }

    TopBar(
        stringResource(R.string.code_new),
        leading = { IconCircleButton(ClookieIcons.Back, stringResource(R.string.action_back), onBack) },
    )
    Spacer(Modifier.height(Space.s3))
    if (pattern < 0) {
        Txt(stringResource(R.string.code_space_full), Clookie.type.heading, color = c.inkMuted)
        return
    }
    if (scanning) {
        val scanner = remember(pattern) { CookieScanner(listOf(CookieCode(1, name, pattern))) }
        LaunchedEffect(scanner.accepted) {
            if (scanner.accepted != ScanGate.NoMatch) {
                haptics.confirm()
                tested = true
                scanning = false
            }
        }
        ScanWindow(
            scanner.streak, scanner.required, if (scanner.wrong) stringResource(R.string.scan_wrong) else null, c.ink, c.surface,
            Modifier.widthIn(max = 320.dp).fillMaxWidth(),
        ) {
            CameraFeed(scanner, torch, onError = { scanning = false }, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(Space.s3))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
            PillChip(stringResource(R.string.scan_torch), selected = torch, icon = ClookieIcons.Torch, onClick = { torch = !torch })
            PillChip(stringResource(R.string.action_close), selected = false, icon = ClookieIcons.Close, onClick = { scanning = false })
        }
    } else {
        DrawingGuide(pattern)
    }
    Spacer(Modifier.height(Space.s4))
    NameField(name, { name = it }, Modifier.fillMaxWidth())
    Spacer(Modifier.height(Space.s3))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s3)) {
        CookieButton(
            stringResource(R.string.code_shuffle), {
                CookiePattern.generate(others)?.let { pattern = it }
            },
            Modifier.weight(1f), kind = ButtonKind.Secondary, icon = ClookieIcons.Repeat, enabled = !tested,
        )
        CookieButton(
            stringResource(if (tested) R.string.code_test_ok else R.string.code_test), {
                if (!inspection) askCamera(listOf(Manifest.permission.CAMERA))
            },
            Modifier.weight(1f), kind = ButtonKind.Secondary, icon = if (tested) ClookieIcons.Check else ClookieIcons.Camera,
        )
    }
    if (denied) {
        Spacer(Modifier.height(Space.s2))
        Txt(stringResource(R.string.camera_needed), Clookie.type.label, color = c.accent)
    }
    Spacer(Modifier.height(Space.s3))
    CookieButton(
        stringResource(R.string.action_save), { onSave(name.trim().ifEmpty { defaultName }, pattern) },
        Modifier.fillMaxWidth(), icon = ClookieIcons.Check, enabled = tested,
    )
}

/** The guide card: paper with the pen animation, then play/pause, step back/forward, "2/5". */
@Composable
private fun DrawingGuide(pattern: Int) {
    val c = Clookie.colors
    val reduced = Clookie.motion.reduced
    val total = guideSteps(pattern).size
    var step by rememberSaveable(pattern) { mutableIntStateOf(0) }
    var playing by rememberSaveable(pattern) { mutableStateOf(!reduced) }
    val progress = remember(pattern) { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(step, playing, pattern) {
        if (!playing) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        // The frame has 16 cells, each pattern step one.
        progress.animateTo(1f, tween(if (step == 0) 6400 else 1400, easing = LinearEasing))
        delay(500)
        if (step < total - 1) step++ else {
            delay(1500)
            step = 0
        }
    }
    val shape = Clookie.shapes.large
    Box(
        Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .border(Stroke.regular, c.outline, shape)
            .padding(Space.s2),
    ) {
        CookieGuide(pattern, step, { progress.value }, Modifier.fillMaxWidth())
    }
    Spacer(Modifier.height(Space.s3))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s3)) {
        IconCircleButton(ClookieIcons.Back, stringResource(R.string.cd_step_back), {
            playing = false
            step = (step - 1).coerceAtLeast(0)
        })
        IconCircleButton(
            if (playing) ClookieIcons.Pause else ClookieIcons.Play,
            stringResource(if (playing) R.string.cd_pause else R.string.cd_play),
            { if (!reduced) playing = !playing },
            filled = true,
        )
        IconCircleButton(ClookieIcons.Chevron, stringResource(R.string.cd_step_next), {
            playing = false
            step = (step + 1).coerceAtMost(total - 1)
        })
        Spacer(Modifier.width(Space.s2))
        Txt(stringResource(R.string.guide_step, step + 1, total), Clookie.type.displaySmall)
    }
}

/** Printable PNG: the marker in black on white with one empty cell around it. */
fun cookieCodeBitmap(pattern: Int, cellPx: Int = 160): Bitmap {
    val n = CookiePattern.Size + 2
    val bitmap = Bitmap.createBitmap(cellPx * n, cellPx * n, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = Paint().apply { color = android.graphics.Color.BLACK }
    for (r in 0 until CookiePattern.Size) for (col in 0 until CookiePattern.Size) {
        if (CookiePattern.markerCell(pattern, r, col)) {
            val x = (col + 1) * cellPx.toFloat()
            val y = (r + 1) * cellPx.toFloat()
            canvas.drawRect(x, y, x + cellPx, y + cellPx, paint)
        }
    }
    return bitmap
}

private fun writeCookieCodePng(context: Context, uri: Uri, pattern: Int) {
    context.contentResolver.openOutputStream(uri)?.use { cookieCodeBitmap(pattern).compress(Bitmap.CompressFormat.PNG, 100, it) }
}

// ---------------------------------------------------------------- QR / barcode

@Composable
internal fun QrEditor(spec: MissionSpec, onChange: (MissionSpec) -> Unit) {
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    val scope = rememberCoroutineScope()
    val codes = rememberScanCodes().orEmpty()
    var open by rememberSaveable { mutableStateOf(false) }
    val id = spec.param(KeyCode, 0).toLong()
    val selected = if (codes.any { it.id == id }) id else 0L
    CodeChips(
        codes.map { it.id to it.name }, selected, { onChange(spec.withCode(it)) },
        stringResource(if (codes.isEmpty()) R.string.qr_add else R.string.code_mine), { open = true },
    )
    if (!open || inspection) return
    val repo = CodeRepository.get(context)
    ClookieOverlay(onDismiss = { open = false }) {
        QrCodesScreen(
            codes = codes,
            onClose = { open = false },
            onSave = { name, d -> scope.launch { onChange(spec.withCode(repo.addScanCode(name, d.format, d.value))) } },
            onDelete = { scope.launch { repo.deleteScanCode(it.id) } },
        )
    }
}

@Composable
fun formatLabel(format: String): String = when (format) {
    "QR_CODE" -> stringResource(R.string.format_qr)
    "EAN_13" -> stringResource(R.string.format_ean13)
    "EAN_8" -> stringResource(R.string.format_ean8)
    "UPC_E" -> stringResource(R.string.format_upce)
    "CODE_128" -> stringResource(R.string.format_code128)
    else -> format
}

/** Registered QR codes and barcodes: list (delete) and "Thêm mã": scan any code, name it, save. */
@Composable
fun QrCodesScreen(
    codes: List<ScanCode>,
    onClose: () -> Unit,
    onSave: (String, Decoded) -> Unit,
    onDelete: (ScanCode) -> Unit,
    modifier: Modifier = Modifier,
    startScanning: Boolean = codes.isEmpty(),
    sample: Decoded? = null,
) {
    val c = Clookie.colors
    val inspection = LocalInspectionMode.current
    val haptics = rememberHaptics()
    var scanning by rememberSaveable { mutableStateOf(startScanning) }
    var denied by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }
    val askCamera = rememberPermissionRequest { ok ->
        denied = !ok
        scanning = ok
    }
    Column(
        modifier
            .fillMaxSize()
            .screenBackground()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.gutter, vertical = Space.s2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!scanning) {
            TopBar(
                stringResource(R.string.mission_qr),
                leading = { IconCircleButton(ClookieIcons.Close, stringResource(R.string.action_close), onClose) },
            )
            Spacer(Modifier.height(Space.s4))
            Column(verticalArrangement = Arrangement.spacedBy(Space.s3)) {
                codes.forEach { code ->
                    CodeRow(
                        icon = { ClookieIcon(ClookieIcons.Qr, null, size = 36.dp) },
                        name = code.name,
                        detail = "${formatLabel(code.format)} · ${code.value}",
                        onDelete = { onDelete(code) },
                    )
                }
            }
            if (denied) {
                Spacer(Modifier.height(Space.s3))
                Txt(stringResource(R.string.camera_needed), Clookie.type.label, color = c.accent)
            }
            Spacer(Modifier.height(Space.s5))
            CookieButton(
                stringResource(R.string.qr_add), { if (!inspection) askCamera(listOf(Manifest.permission.CAMERA)) },
                Modifier.fillMaxWidth(), icon = ClookieIcons.Plus,
            )
        } else {
            TopBar(
                stringResource(R.string.qr_add),
                leading = { IconCircleButton(ClookieIcons.Back, stringResource(R.string.action_back), { scanning = false }) },
            )
            Spacer(Modifier.height(Space.s3))
            val scanner = remember { BarcodeScanner(null) }
            val found = sample ?: scanner.found
            LaunchedEffect(found) { if (found != null) haptics.confirm() }
            if (found == null) {
                ScanWindow(scanner.streak, scanner.required, null, c.ink, c.surface, Modifier.widthIn(max = 320.dp).fillMaxWidth()) {
                    CameraFeed(scanner, torch, onError = { scanning = false }, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(Space.s3))
                Txt(stringResource(R.string.qr_point), Clookie.type.label, color = c.inkMuted)
                Spacer(Modifier.height(Space.s3))
                PillChip(stringResource(R.string.scan_torch), selected = torch, icon = ClookieIcons.Torch, onClick = { torch = !torch })
            } else {
                val defaultName = stringResource(R.string.code_default_name, codes.size + 1)
                var name by rememberSaveable(found) { mutableStateOf(defaultName) }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(c.surface, Clookie.shapes.large)
                        .border(Stroke.regular, c.outline, Clookie.shapes.large)
                        .padding(Space.s5),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ClookieIcon(ClookieIcons.Qr, null, size = 56.dp)
                    Spacer(Modifier.height(Space.s3))
                    Eyebrow(formatLabel(found.format))
                    Txt(found.value, Clookie.type.title, maxLines = 2)
                }
                Spacer(Modifier.height(Space.s4))
                NameField(name, { name = it }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(Space.s4))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s3)) {
                    CookieButton(stringResource(R.string.qr_again), { scanner.restart() }, Modifier.weight(1f), kind = ButtonKind.Secondary)
                    CookieButton(
                        stringResource(R.string.action_save), {
                            onSave(name.trim().ifEmpty { defaultName }, found)
                            scanner.restart()
                            scanning = false
                        },
                        Modifier.weight(1f), icon = ClookieIcons.Check,
                    )
                }
            }
        }
        Spacer(Modifier.height(Space.s6))
    }
}
