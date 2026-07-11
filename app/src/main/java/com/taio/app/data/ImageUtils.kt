package com.taio.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * نفس منطق compressImageFile في الموقع بالظبط:
 * أقصى بعد 400px، جودة JPEG 75%، وتخزين النتيجة كـ Data URL base64 جوه مستند المنتج نفسه
 * (مفيش Firebase Storage — بالظبط زي ما الموقع شغال).
 */
object ImageUtils {
    fun compressToDataUrl(context: Context, uri: Uri, maxDim: Int = 400, quality: Int = 75): String? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(input)
            input.close()
            if (original == null) return null

            var w = original.width
            var h = original.height
            if (w > maxDim || h > maxDim) {
                if (w > h) {
                    h = (h * maxDim.toDouble() / w).toInt()
                    w = maxDim
                } else {
                    w = (w * maxDim.toDouble() / h).toInt()
                    h = maxDim
                }
            }
            val resized = Bitmap.createScaledBitmap(original, w, h, true)
            val out = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (e: Exception) {
            null
        }
    }

    fun decodeDataUrl(dataUrl: String?): Bitmap? {
        if (dataUrl.isNullOrBlank()) return null
        return try {
            val base64 = if (dataUrl.contains("base64,")) dataUrl.substringAfter("base64,") else dataUrl
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
