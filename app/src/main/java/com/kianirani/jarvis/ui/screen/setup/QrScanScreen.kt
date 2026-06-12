package com.kianirani.jarvis.ui.screen.setup

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn as AndroidxOptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.kianirani.jarvis.brain.discovery.JoinPayload
import com.kianirani.jarvis.ui.theme.VisionColors
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Camera QR scanner for setup-wizard pairing. CameraX preview + ML Kit barcode
 * analysis (QR only). On the first barcode whose rawValue decodes via
 * [JoinPayload.decode] != null, analysis stops and [onScanned] fires once.
 *
 * Styled to the Vision HUD: cyan scan frame with an animated scan line over a
 * dimmed camera feed. Lifecycle-safe — camera is unbound, the ML Kit scanner is
 * closed, and the analysis executor is shut down on dispose.
 */
@Composable
fun QrScanScreen(
    onScanned: (JoinPayload) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted = it }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        if (granted) {
            CameraScanner(onScanned = onScanned)
            ScanOverlay()
        } else {
            PermissionPrompt(onRequest = { launcher.launch(Manifest.permission.CAMERA) })
        }

        Text(
            "CANCEL",
            style = MaterialTheme.typography.labelLarge,
            color = VisionColors.CyanSecondary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onCancel)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun CameraScanner(onScanned: (JoinPayload) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    DisposableEffect(Unit) {
        val executor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build(),
        )
        val consumed = AtomicBoolean(false)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { proxy ->
                processFrame(scanner, proxy) { raw ->
                    val payload = JoinPayload.decode(raw)
                    if (payload != null && consumed.compareAndSet(false, true)) {
                        analysis.clearAnalyzer()
                        previewView.post { onScanned(payload) }
                    }
                }
            }
            runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
            runCatching { scanner.close() }
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

@AndroidxOptIn(ExperimentalGetImage::class)
private fun processFrame(
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    proxy: ImageProxy,
    onRaw: (String) -> Unit,
) {
    val mediaImage = proxy.image
    if (mediaImage == null) {
        proxy.close()
        return
    }
    val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            barcodes.firstNotNullOfOrNull { it.rawValue }?.let(onRaw)
        }
        .addOnCompleteListener { proxy.close() }
}

/** Cyan HUD frame with an animated scan line over a dimmed feed. */
@Composable
private fun ScanOverlay() {
    val transition = rememberInfiniteTransition(label = "qrScan")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "qrSweep",
    )
    Column(
        Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "ALIGN BRAIN QR IN FRAME",
            style = MaterialTheme.typography.headlineMedium,
            color = VisionColors.CyanSecondary,
        )
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .drawBehind {
                    val stroke = 2.5.dp.toPx()
                    val len = size.minDimension * 0.18f
                    val cyan = VisionColors.CyanPrimary
                    // corner brackets
                    val corners = listOf(
                        Offset(0f, 0f) to listOf(Offset(len, 0f), Offset(0f, len)),
                        Offset(size.width, 0f) to listOf(Offset(size.width - len, 0f), Offset(size.width, len)),
                        Offset(0f, size.height) to listOf(Offset(len, size.height), Offset(0f, size.height - len)),
                        Offset(size.width, size.height) to listOf(
                            Offset(size.width - len, size.height),
                            Offset(size.width, size.height - len),
                        ),
                    )
                    corners.forEach { (o, ends) -> ends.forEach { drawLine(cyan, o, it, stroke) } }
                    // animated scan line
                    val y = sweep * size.height
                    drawLine(
                        color = cyan.copy(alpha = 0.85f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.5.dp.toPx(),
                    )
                    drawRect(
                        color = VisionColors.CyanGlow,
                        topLeft = Offset(0f, y - 14f),
                        size = Size(size.width, 28f),
                    )
                },
        )
    }
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "CAMERA ACCESS REQUIRED",
            style = MaterialTheme.typography.headlineLarge,
            color = VisionColors.CyanSecondary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Grant camera permission to scan your Brain's pairing QR code.",
            style = MaterialTheme.typography.bodyMedium,
            color = VisionColors.TextDim,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "GRANT ACCESS",
            style = MaterialTheme.typography.labelLarge,
            color = VisionColors.CyanPrimary,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(VisionColors.Surface)
                .clickable(onClick = onRequest)
                .padding(horizontal = 24.dp, vertical = 14.dp),
        )
    }
}
