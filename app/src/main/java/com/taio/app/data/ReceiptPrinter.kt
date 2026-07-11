package com.taio.app.data

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * بيولّد إيصال أوردر بمقاس A4 (نفس شكل إيصال الموقع بالظبط: هيدر بالبراند + رقم البوليصة،
 * بيانات المرسل والمستلم، جدول المنتجات، صندوق الإجمالي، وخط القص) ويطبعه عن طريق
 * نظام الطباعة في أندرويد (اللي بيدي المستخدم خيار طباعة فعلية أو حفظ كـ PDF).
 */
object ReceiptPrinter {

    // A4 بالنقاط (72 نقطة/بوصة): 210mm × 297mm
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 36f

    private fun buildPdf(order: Order): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val ink = Color.parseColor("#34383C")
        val dim = Color.parseColor("#8C8880")
        val cream = Color.parseColor("#F6F4EF")
        val line = Color.parseColor("#E4E1D9")

        val cardLeft = MARGIN
        val cardRight = PAGE_W - MARGIN
        val cardWidth = cardRight - cardLeft
        var y = MARGIN + 24f

        // ===== Header: البراند + رقم البوليصة =====
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = 26f; isFakeBoldText = true; textAlign = Paint.Align.LEFT
        }
        canvas.drawText("TAIO", cardLeft, y, brandPaint)

        val orderNoLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dim; textSize = 11f; textAlign = Paint.Align.RIGHT
        }
        val orderNoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = 16f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("رقم البوليصة", cardRight, y - 16f, orderNoLabelPaint)
        canvas.drawText(order.orderNo, cardRight, y + 4f, orderNoPaint)

        y += 20f
        val headerLinePaint = Paint().apply { color = ink; strokeWidth = 2f }
        canvas.drawLine(cardLeft, y, cardRight, y, headerLinePaint)
        y += 34f

        // ===== المرسل / المستلم =====
        val blockTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dim; textSize = 10f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT
        }
        val blockTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = 12.5f; textAlign = Paint.Align.RIGHT
        }

        val rightColX = cardRight
        val leftColX = cardLeft + cardWidth / 2f - 10f
        val blockTop = y

        canvas.drawText("المرسل", rightColX, y, blockTitlePaint)
        canvas.drawText("TAIO Accessories", rightColX, y + 18f, blockTextPaint)
        canvas.drawText("Egypt", rightColX, y + 34f, blockTextPaint)

        canvas.drawText("المستلم", leftColX, blockTop, blockTitlePaint)
        canvas.drawText(order.name.ifBlank { "—" }, leftColX, blockTop + 18f, blockTextPaint)
        canvas.drawText(order.phone.ifBlank { "—" }, leftColX, blockTop + 34f, blockTextPaint)
        canvas.drawText(order.address.ifBlank { "—" }, leftColX, blockTop + 50f, blockTextPaint)
        canvas.drawText(order.city.ifBlank { "—" }, leftColX, blockTop + 66f, blockTextPaint)

        y = blockTop + 92f

        // ===== جدول المنتجات =====
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dim; textSize = 10f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT
        }
        val tableTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = 12f; textAlign = Paint.Align.RIGHT
        }
        val tableLinePaint = Paint().apply { color = line; strokeWidth = 1f }

        val colProduct = cardRight
        val colQty = cardRight - cardWidth * 0.45f
        val colPrice = cardRight - cardWidth * 0.65f
        val colTotal = cardLeft + 4f

        canvas.drawText("المنتج", colProduct, y, tableHeaderPaint)
        canvas.drawText("الكمية", colQty, y, tableHeaderPaint)
        canvas.drawText("السعر", colPrice, y, tableHeaderPaint)
        canvas.drawText("الإجمالي", colTotal + 40f, y, Paint(tableHeaderPaint).apply { textAlign = Paint.Align.LEFT })
        y += 8f
        canvas.drawLine(cardLeft, y, cardRight, y, tableLinePaint)
        y += 22f

        order.items.forEach { item ->
            canvas.drawText(item.name, colProduct, y, tableTextPaint)
            canvas.drawText(item.qty.toString(), colQty, y, tableTextPaint)
            canvas.drawText("${item.price.toInt()}", colPrice, y, tableTextPaint)
            canvas.drawText(
                "${item.lineTotal.toInt()}", colTotal + 40f, y,
                Paint(tableTextPaint).apply { textAlign = Paint.Align.LEFT }
            )
            y += 20f
            canvas.drawLine(cardLeft, y - 8f, cardRight, y - 8f, tableLinePaint)
        }

        y += 18f

        // ===== صندوق الإجمالي =====
        val boxHeight = 60f
        val boxPaint = Paint().apply { color = cream; style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(cardLeft, y, cardRight, y + boxHeight), 10f, 10f, boxPaint)

        val smallDimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dim; textSize = 10.5f; textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("المطلوب تحصيله (شامل الشحن)", cardRight - 16f, y + 22f, smallDimPaint)
        canvas.drawText("شحن: ${order.shipping.toInt()} جنيه", cardRight - 16f, y + 40f, smallDimPaint)

        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink; textSize = 20f; isFakeBoldText = true; textAlign = Paint.Align.LEFT
        }
        canvas.drawText("${order.grandTotal.toInt()} جنيه", cardLeft + 16f, y + 38f, amountPaint)

        y += boxHeight + 20f

        if (order.notes.isNotBlank()) {
            canvas.drawText("ملاحظات: ${order.notes}", cardRight, y, smallDimPaint)
            y += 20f
        }

        // ===== خط القص =====
        y += 20f
        val dashPaint = Paint().apply { color = dim; strokeWidth = 1.2f; pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f, 6f), 0f) }
        canvas.drawLine(cardLeft, y, cardRight, y, dashPaint)
        val cutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dim; textSize = 11f; textAlign = Paint.Align.CENTER }
        canvas.drawText("✂ قص هنا", PAGE_W / 2f, y - 6f, cutPaint)

        doc.finishPage(page)

        val outFile = File.createTempFile("TAIO_${order.orderNo}_", ".pdf")
        FileOutputStream(outFile).use { out -> doc.writeTo(out) }
        doc.close()
        return outFile
    }

    /** بتفتح نافذة الطباعة في أندرويد (فيها خيار حفظ كـ PDF أو طباعة فعلية على أي طابعة متاحة) */
    fun printReceipt(context: Context, order: Order) {
        val file = buildPdf(order)
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: android.os.Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("TAIO_${order.orderNo}.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, oldAttributes != newAttributes)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                try {
                    FileInputStream(file).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        val attrs = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        printManager.print("TAIO_${order.orderNo}", adapter, attrs)
    }

    /** بديل: مشاركة ملف الـ PDF مباشرة (واتساب، إيميل، حفظ في الملفات...) */
    fun shareReceipt(context: Context, order: Order) {
        val file = buildPdf(order)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة إيصال الأوردر"))
    }
}
