package com.stemtutor.feature.notes.capture

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.PromptScreen
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.feature.notes.R
import java.io.File
import com.stemtutor.core.designsystem.R as DsR

/**
 * Camera for several shots in a row. Photos are saved to the app's cache; [onDone] receives
 * their file URIs. Asks for camera permission with a bilingual reason first.
 */
@Composable
fun CaptureRoute(maxShots: Int, onDone: (List<String>) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    if (!granted) {
        CameraPermissionScreen(onAllow = { request.launch(Manifest.permission.CAMERA) }, onBack = onBack)
        return
    }
    val shots = remember { mutableStateListOf<String>() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val capture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    var busy by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
        }, ContextCompat.getMainExecutor(context))
        onDispose { runCatching { future.get().unbindAll() } }
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        }
        Column(
            Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BilingualText(bilingualStringResource(R.string.nt_photos_taken, shots.size))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChunkyButton(
                    R.string.nt_take_photo,
                    {
                        busy = true
                        val file = File(context.cacheDir, "notes/page_${System.currentTimeMillis()}.jpg").apply { parentFile?.mkdirs() }
                        capture.takePicture(
                            ImageCapture.OutputFileOptions.Builder(file).build(),
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    shots += Uri.fromFile(file).toString()
                                    busy = false
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    busy = false
                                }
                            },
                        )
                    },
                    Modifier.weight(1f),
                    enabled = !busy && shots.size < maxShots,
                )
                ChunkyButton(DsR.string.ds_done, { onDone(shots.toList()) }, Modifier.weight(1f), ChunkyButtonStyle.White)
            }
        }
    }
}


@Composable
fun CameraPermissionScreen(onAllow: () -> Unit, onBack: () -> Unit, animate: Boolean = true) {
    PromptScreen(
        bubble = bilingualStringResource(R.string.nt_camera_why),
        pose = CowPose.Question,
        animate = animate,
        footer = {
            ChunkyButton(R.string.nt_allow_camera, onAllow, Modifier.fillMaxWidth())
            ChunkyButton(DsR.string.ds_back, onBack, Modifier.fillMaxWidth(), ChunkyButtonStyle.Ghost)
        },
    )
}

@BilingualPreviews
@Composable
private fun CameraPermissionPreview() {
    StemTutorTheme { CameraPermissionScreen({}, {}, animate = false) }
}
