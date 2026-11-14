package com.stemtutor.core.designsystem.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** A scannable QR code for [content] (the class join link), drawn with ZXing. */
@Composable
fun QrCode(
    content: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
) {
    val bitmap = remember(content) { qrBitmap(content) }
    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        filterQuality = FilterQuality.None,
        modifier = modifier.size(size),
    )
}

private const val DARK = 0xFF1F6F88.toInt()
private const val LIGHT = 0xFFFFFFFF.toInt()

fun qrBitmap(content: String, pixels: Int = 256): ImageBitmap {
    val hints = mapOf(
        EncodeHintType.MARGIN to 1,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.CHARACTER_SET to "UTF-8",
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, pixels, pixels, hints)
    val colors = IntArray(matrix.width * matrix.height) { i ->
        if (matrix.get(i % matrix.width, i / matrix.width)) DARK else LIGHT
    }
    return Bitmap.createBitmap(colors, matrix.width, matrix.height, Bitmap.Config.ARGB_8888).asImageBitmap()
}
