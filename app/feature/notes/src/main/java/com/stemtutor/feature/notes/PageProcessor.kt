package com.stemtutor.feature.notes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.stemtutor.core.data.model.PageKind
import com.stemtutor.core.data.model.PageUpload
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

/** A crop as fractions of the (rotated) image: 0..1 on each side. */
data class CropRect(val left: Float = 0f, val top: Float = 0f, val right: Float = 1f, val bottom: Float = 1f) {
    val isFull get() = left <= 0f && top <= 0f && right >= 1f && bottom >= 1f

    companion object {
        val FULL = CropRect()
        const val MIN_SIZE = 0.15f
    }
}

/** One page the user is assembling before upload. */
data class PageDraft(
    val id: String,
    val kind: PageKind,
    val uri: String? = null,
    val text: String? = null,
    val rotation: Int = 0,
    val crop: CropRect = CropRect.FULL,
    val sizeBytes: Long = 0,
)

/** Pure image maths, kept free of Android types so it's unit-testable. */
object NotesImages {
    const val MAX_SIDE = 1600
    const val JPEG_QUALITY = 85
    const val MAX_PDF_BYTES = 10L * 1024 * 1024

    /** Below this Laplacian variance (on a ~256 px greyscale copy) a photo counts as blurry. */
    const val BLUR_THRESHOLD = 60.0

    /** Size after scaling so the long side is at most [maxSide] (never upscales). */
    fun scaledSize(width: Int, height: Int, maxSide: Int = MAX_SIDE): Pair<Int, Int> {
        val long = max(width, height)
        if (long <= maxSide) return width to height
        val k = maxSide.toDouble() / long
        return (width * k).roundToInt().coerceAtLeast(1) to (height * k).roundToInt().coerceAtLeast(1)
    }

    /** Variance of the 3×3 Laplacian over a greyscale image: low means few sharp edges. */
    fun laplacianVariance(gray: IntArray, width: Int, height: Int): Double {
        if (width < 3 || height < 3) return 0.0
        var sum = 0.0
        var sumSq = 0.0
        var n = 0
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val i = y * width + x
                val v = (gray[i - width] + gray[i + width] + gray[i - 1] + gray[i + 1] - 4 * gray[i]).toDouble()
                sum += v
                sumSq += v * v
                n++
            }
        }
        val mean = sum / n
        return sumSq / n - mean * mean
    }

    /** The crop in pixels for an image of [width] × [height]. */
    fun cropPixels(crop: CropRect, width: Int, height: Int): IntArray {
        val l = (crop.left.coerceIn(0f, 1f) * width).roundToInt()
        val t = (crop.top.coerceIn(0f, 1f) * height).roundToInt()
        val r = (crop.right.coerceIn(0f, 1f) * width).roundToInt().coerceAtLeast(l + 1)
        val b = (crop.bottom.coerceIn(0f, 1f) * height).roundToInt().coerceAtLeast(t + 1)
        return intArrayOf(l, t, (r - l).coerceAtMost(width - l), (b - t).coerceAtMost(height - t))
    }
}

/** Turns drafts into upload-ready pages. An interface so ViewModel tests can use a fake. */
interface PageProcessor {
    suspend fun isBlurry(uri: String): Boolean

    suspend fun sizeOf(uri: String): Long

    suspend fun prepare(page: PageDraft): PageUpload
}

class AndroidPageProcessor @Inject constructor(@ApplicationContext private val context: Context) : PageProcessor {
    override suspend fun isBlurry(uri: String): Boolean = withContext(Dispatchers.Default) {
        val bitmap = decode(Uri.parse(uri), 256) ?: return@withContext false
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h).also { bitmap.getPixels(it, 0, w, 0, 0, w, h) }
        bitmap.recycle()
        val gray = IntArray(pixels.size) { i ->
            val p = pixels[i]
            ((p shr 16 and 0xFF) * 299 + (p shr 8 and 0xFF) * 587 + (p and 0xFF) * 114) / 1000
        }
        NotesImages.laplacianVariance(gray, w, h) < NotesImages.BLUR_THRESHOLD
    }

    override suspend fun sizeOf(uri: String): Long = withContext(Dispatchers.IO) {
        context.contentResolver.query(Uri.parse(uri), arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getLong(0) else 0L
        } ?: 0L
    }

    override suspend fun prepare(page: PageDraft): PageUpload = withContext(Dispatchers.Default) {
        when (page.kind) {
            PageKind.TEXT -> PageUpload(PageKind.TEXT, text = page.text.orEmpty())
            PageKind.PDF -> PageUpload(PageKind.PDF, bytes = read(Uri.parse(requireNotNull(page.uri))))
            PageKind.PHOTO -> PageUpload(PageKind.PHOTO, bytes = compressPhoto(page))
        }
    }

    private fun read(uri: Uri): ByteArray =
        requireNotNull(context.contentResolver.openInputStream(uri)) { "cannot open $uri" }.use { it.readBytes() }

    private fun compressPhoto(page: PageDraft): ByteArray {
        val uri = Uri.parse(requireNotNull(page.uri))
        val decoded = requireNotNull(decode(uri, NotesImages.MAX_SIDE * 2)) { "cannot decode $uri" }
        val rotation = (exifRotation(uri) + page.rotation) % 360
        var bitmap = if (rotation != 0) rotate(decoded, rotation) else decoded
        if (!page.crop.isFull) {
            val (x, y, w, h) = NotesImages.cropPixels(page.crop, bitmap.width, bitmap.height).toList()
            bitmap = Bitmap.createBitmap(bitmap, x, y, w, h)
        }
        val (tw, th) = NotesImages.scaledSize(bitmap.width, bitmap.height)
        if (tw != bitmap.width) bitmap = Bitmap.createScaledBitmap(bitmap, tw, th, true)
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, NotesImages.JPEG_QUALITY, out)
            out.toByteArray()
        }
    }

    /** Decodes with power-of-two sampling so the long side is about [target] px or more. */
    private fun decode(uri: Uri, target: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= target) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    private fun exifRotation(uri: Uri): Int = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    }.getOrDefault(0)

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap =
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
}
