package com.stemtutor.feature.student.join

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.stemtutor.core.data.classroom.JoinKeys
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.PromptScreen
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.feature.student.R
import java.util.concurrent.Executors
import com.stemtutor.core.designsystem.R as DsR

/**
 * Scans the class QR code with CameraX + ML Kit (on-device, works offline). Asks for camera
 * permission with a bilingual reason first; the camera is only used on this screen.
 */
@Composable
fun ScanQrRoute(onKey: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    ScanQrScreen(granted = granted, onAllow = { request.launch(Manifest.permission.CAMERA) }, onKey = onKey, onBack = onBack)
}

@Composable
fun ScanQrScreen(granted: Boolean, onAllow: () -> Unit, onKey: (String) -> Unit, onBack: () -> Unit, animate: Boolean = true) {
    PromptScreen(
        bubble = bilingualStringResource(if (granted) R.string.st_scan_hint else R.string.st_camera_why),
        pose = CowPose.Question,
        animate = animate && !granted,
        footer = {
            if (!granted) ChunkyButton(R.string.st_camera_allow, onAllow, Modifier.fillMaxWidth())
            ChunkyButton(DsR.string.ds_back, onBack, Modifier.fillMaxWidth(), ChunkyButtonStyle.Ghost)
        },
    ) {
        if (granted) {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .padding(4.dp),
                ) { CameraScanner(onKey) }
            }
        }
    }
}

@Composable
private fun CameraScanner(onKey: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnKey by rememberUpdatedState(onKey)
    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    }
    var delivered by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { proxy ->
                analyze(proxy, scanner) { text ->
                    val key = JoinKeys.findIn(text)
                    if (key != null && !delivered) {
                        delivered = true
                        ContextCompat.getMainExecutor(context).execute { latestOnKey(key) }
                    }
                }
            }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            scanner.close()
            executor.shutdown()
        }
    }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
}

@OptIn(ExperimentalGetImage::class)
private fun analyze(proxy: ImageProxy, scanner: com.google.mlkit.vision.barcode.BarcodeScanner, onText: (String) -> Unit) {
    val media = proxy.image
    if (media == null) {
        proxy.close()
        return
    }
    val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnSuccessListener { codes -> codes.firstNotNullOfOrNull { it.rawValue }?.let(onText) }
        .addOnCompleteListener { proxy.close() }
}

@BilingualPreviews
@Composable
private fun ScanQrPreview() {
    StemTutorTheme { ScanQrScreen(granted = false, onAllow = {}, onKey = {}, onBack = {}, animate = false) }
}
