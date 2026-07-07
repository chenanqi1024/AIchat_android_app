package vibe.ccc.aichat.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class CompressedImageAttachment(
    val data: ByteArray,
    val dataUrl: String
)

class ChatImageCompressionException(message: String) : Exception(message)

object ImageCompressor {
    private const val MAX_SIDE = 1440f
    private const val MAX_BYTES = 6 * 1024 * 1024
    private val qualities = listOf(86, 78, 70, 62, 55, 48)

    fun compress(contentResolver: ContentResolver, uri: Uri): CompressedImageAttachment {
        val originalData = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw ChatImageCompressionException("无法读取这张图片")
        val originalBitmap = BitmapFactory.decodeByteArray(originalData, 0, originalData.size)
            ?: throw ChatImageCompressionException("无法读取这张图片")

        val resized = resize(originalBitmap)
        qualities.forEach { quality ->
            val output = ByteArrayOutputStream()
            if (resized.compress(Bitmap.CompressFormat.JPEG, quality, output)) {
                val bytes = output.toByteArray()
                if (bytes.size <= MAX_BYTES) {
                    return CompressedImageAttachment(
                        data = bytes,
                        dataUrl = "data:image/jpeg;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
                    )
                }
            }
        }

        throw ChatImageCompressionException("图片压缩后仍超过 6MB，请换一张更小的图片")
    }

    private fun resize(bitmap: Bitmap): Bitmap {
        val longSide = max(bitmap.width, bitmap.height).toFloat()
        if (longSide <= 0f) return bitmap

        val ratio = min(1f, MAX_SIDE / longSide)
        val targetWidth = max(1, floor(bitmap.width * ratio).toInt())
        val targetHeight = max(1, floor(bitmap.height * ratio).toInt())

        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        Canvas(output).apply {
            drawColor(Color.WHITE)
            drawBitmap(scaled, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }
        return output
    }
}
