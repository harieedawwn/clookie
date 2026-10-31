package io.github.harieedawwn.clookie.scan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Size
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

fun hasCamera(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

/**
 * Back camera preview plus luminance frames for [sink] at analysis resolution (640x480 or the
 * closest size), on one background thread. Analysis keeps only the latest frame, so slow frames
 * are dropped instead of queued. [onError] fires when the camera cannot be opened or sends no
 * frame for [StallMs] while the screen is visible. In previews and screenshots nothing starts.
 */
@Composable
fun CameraFeed(
    sink: FrameSink,
    torch: Boolean,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
    onTorchAvailable: (Boolean) -> Unit = {},
) {
    if (LocalInspectionMode.current) {
        Box(modifier)
        return
    }
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val error by rememberUpdatedState(onError)
    val torchAvailable by rememberUpdatedState(onTorchAvailable)
    var request by remember { mutableStateOf<SurfaceRequest?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(executor) { onDispose { executor.shutdown() } }

    LaunchedEffect(owner, sink) {
        val provider = try {
            ProcessCameraProvider.awaitInstance(context)
        } catch (_: Exception) {
            error()
            return@LaunchedEffect
        }
        val preview = Preview.Builder().build().apply { setSurfaceProvider { request = it } }
        val lastFrame = AtomicLong(SystemClock.elapsedRealtime())
        val copier = YPlane()
        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                    .build(),
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(executor) { image ->
            lastFrame.set(SystemClock.elapsedRealtime())
            try {
                copier.copy(image)
                sink.onFrame(copier.bytes, image.width, image.height, copier.rowStride)
            } finally {
                image.close()
            }
        }
        val bound = try {
            provider.unbindAll()
            provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        } catch (_: Exception) {
            null
        }
        if (bound == null) {
            error()
            return@LaunchedEffect
        }
        camera = bound
        torchAvailable(bound.cameraInfo.hasFlashUnit())
        try {
            while (true) {
                delay(1000)
                val now = SystemClock.elapsedRealtime()
                if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    lastFrame.set(now)
                } else if (now - lastFrame.get() > StallMs) {
                    error()
                    break
                }
            }
        } finally {
            analysis.clearAnalyzer()
            provider.unbind(preview, analysis)
            camera = null
            request = null
        }
    }
    LaunchedEffect(camera, torch) {
        camera?.let { if (it.cameraInfo.hasFlashUnit()) it.cameraControl.enableTorch(torch) }
    }
    Box(modifier) {
        request?.let { CameraXViewfinder(it, Modifier.fillMaxSize(), implementationMode = ImplementationMode.EMBEDDED) }
    }
}

private const val StallMs = 6000L

/** Copies the Y plane into a reused array (rows keep the plane's stride). */
private class YPlane {
    var bytes = ByteArray(0)
        private set
    var rowStride = 0
        private set

    fun copy(image: ImageProxy) {
        val plane = image.planes[0]
        val buffer = plane.buffer
        buffer.rewind()
        val n = buffer.remaining()
        if (bytes.size < n) bytes = ByteArray(n)
        buffer.get(bytes, 0, n)
        rowStride = plane.rowStride
    }
}
