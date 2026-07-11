package com.taio.app.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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

    /** بتحمل صورة بحجمها شبه الأصلي (مع تصغير بسيط لو كبيرة جداً عشان منقعش في مشاكل ذاكرة) — مستخدمة في أداة الختم */
    fun loadBitmap(context: Context, uri: Uri, maxDim: Int = 2200): Bitmap? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }
            var sample = 1
            while ((boundsOpts.outWidth / sample) > maxDim || (boundsOpts.outHeight / sample) > maxDim) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (e: Exception) {
            null
        }
    }

    /** بيرجع اسم الملف الأصلي للصورة لو موجود (مستخدم في تسمية الصور المصدَّرة من أداة الختم) */
    fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * بتدمج صورة اللوجو فوق صورة المنتج بنفس منطق أداة الختم في الموقع:
     * posX/posY = مركز اللوجو كنسبة من أبعاد الصورة (0..1)، scale = عرض اللوجو كنسبة من عرض الصورة، opacity = الشفافية.
     */
    fun stampLogo(base: Bitmap, logo: Bitmap, posX: Float, posY: Float, scale: Float, opacity: Float): Bitmap {
        val result = base.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val logoW = (result.width * scale).coerceAtLeast(1f)
        val logoH = logoW * (logo.height.toFloat() / logo.width.toFloat())
        val cx = posX * result.width
        val cy = posY * result.height
        val left = cx - logoW / 2f
        val top = cy - logoH / 2f
        val scaledLogo = Bitmap.createScaledBitmap(logo, logoW.toInt().coerceAtLeast(1), logoH.toInt().coerceAtLeast(1), true)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = (opacity.coerceIn(0f, 1f) * 255).toInt()
        }
        canvas.drawBitmap(scaledLogo, left, top, paint)
        return result
    }

    /** بتحفظ الصورة في معرض الصور (Pictures/TAIO) — شغالة من أندرويد 7 لحد أحدث إصدار */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, displayName: String): Boolean {
        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TAIO")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val uri = resolver.insert(collection, values) ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            } ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
