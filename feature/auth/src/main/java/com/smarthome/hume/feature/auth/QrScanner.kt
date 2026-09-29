package com.smarthome.hume.feature.auth

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

/**
 * Quet QR bang camera: CameraX Preview + ImageAnalysis -> MLKit BarcodeScanning.
 * Chi goi [onScanned] 1 lan cho ma dau tien quet duoc; sau do dung phan tich
 * de tranh goi lap lai.
 */
@Composable
fun QrScanner(
    onScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission) {
        Text(
            text = "Cần quyền camera để quét mã QR. Hãy cấp quyền rồi mở lại tab Quét.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 16.dp),
        )
        return
    }

    // Da quet xong -> khong goi onScanned them lan nua.
    var consumed by remember { mutableStateOf(false) }
    val latestOnScanned by rememberUpdatedState(onScanned)
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    // Chi bind camera 1 lan (update chay lai moi recompose).
    var cameraBound by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small),
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            update = { previewView ->
                if (cameraBound) return@AndroidView
                cameraBound = true
                val future = ProcessCameraProvider.getInstance(context)
                future.addListener({
                    val provider = future.get()
                    cameraProviderRef = provider
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val scanner = BarcodeScanning.getClient()
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { ia ->
                            ia.setAnalyzer(
                                ContextCompat.getMainExecutor(context),
                            ) { imageProxy ->
                                if (consumed) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }
                                processImageProxy(scanner, imageProxy) { raw ->
                                    consumed = true
                                    latestOnScanned(raw)
                                }
                            }
                        }
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                    } catch (_: Exception) {
                        // Camera ban hoac dang duoc app khac giu — giu preview trong.
                    }
                }, ContextCompat.getMainExecutor(context))
            },
            modifier = Modifier.fillMaxWidth(),
        )
        // Khung ngam can giua.
        ScanFrameOverlay(Modifier.matchParentSize())
    }

    DisposableEffect(Unit) {
        onDispose { cameraProviderRef?.unbindAll() }
    }
}

/** Ve 4 goc khung ngam kieu viewfinder. */
@Composable
private fun ScanFrameOverlay(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.padding(48.dp)) {
        val len = 64f
        val w = 10f
        val corners = listOf(
            Triple(Offset(0f, 0f), Offset(len, 0f), Offset(0f, len)),
            Triple(Offset(size.width, 0f), Offset(size.width - len, 0f), Offset(size.width, len)),
            Triple(Offset(0f, size.height), Offset(len, size.height), Offset(0f, size.height - len)),
            Triple(
                Offset(size.width, size.height),
                Offset(size.width - len, size.height),
                Offset(size.width, size.height - len),
            ),
        )
        for ((p, h, v) in corners) {
            drawLine(color, p, h, strokeWidth = w, cap = StrokeCap.Round)
            drawLine(color, p, v, strokeWidth = w, cap = StrokeCap.Round)
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    scanner: BarcodeScanner,
    imageProxy: ImageProxy,
    onFound: (String) -> Unit,
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            val raw = barcodes.firstOrNull()?.rawValue
            if (!raw.isNullOrBlank()) onFound(raw)
        }
        .addOnCompleteListener { imageProxy.close() }
}

/** Ghi chu huong dan dat duoi khung quet (dung chung trong LoginScreen). */
@Composable
fun QrScanHint(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Hướng camera vào mã QR chứa địa chỉ + token",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
