package com.wenbo.bookkeeping.data.llm

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * 图片读取：按长边上限降采样 + JPEG 压缩 + base64。
 * 视觉模型不需要原始分辨率，1280px 足够识别票据且显著省 token。
 */
object ImageCodec {

    private const val MAX_DIM = 1280
    private const val JPEG_QUALITY = 85

    /** 返回 (base64, mediaType)；读取或解码失败返回 null。 */
    fun readBase64(context: Context, uri: Uri): Pair<String, String>? {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            } ?: return null
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val sample = sampleSize(bounds.outWidth, bounds.outHeight)
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null

            val scaled = scaleIfNeeded(bitmap)
            val bytes = ByteArrayOutputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.toByteArray()
            }
            if (scaled !== bitmap) bitmap.recycle()
            Base64.encodeToString(bytes, Base64.NO_WRAP) to "image/jpeg"
        }.getOrNull()
    }

    /** 2 的幂采样，使解码后长边不超过约 2 × MAX_DIM（后续再精确缩放）。 */
    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        var longSide = maxOf(width, height)
        while (longSide / 2 >= MAX_DIM) {
            sample *= 2
            longSide /= 2
        }
        return sample
    }

    private fun scaleIfNeeded(bitmap: Bitmap): Bitmap {
        val longSide = maxOf(bitmap.width, bitmap.height)
        if (longSide <= MAX_DIM) return bitmap
        val ratio = MAX_DIM.toFloat() / longSide
        val w = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val h = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }
}
