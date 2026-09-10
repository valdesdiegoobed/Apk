package com.vaguer.pdfeditor

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument as AndroidPdfDocument
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File
import java.io.FileOutputStream

object PdfOps {
    fun copyUriToFile(context: Context, uri: Uri, out: File) {
        context.contentResolver.openInputStream(uri)!!.use { input ->
            FileOutputStream(out).use { output -> input.copyTo(output) }
        }
    }

    fun merge(context: Context, uris: List<Uri>, out: File) {
        PDDocument().use { dst ->
            uris.forEach { uri ->
                context.contentResolver.openInputStream(uri)!!.use { input ->
                    PDDocument.load(input).use { src ->
                        for (i in 0 until src.numberOfPages) dst.importPage(src.getPage(i))
                    }
                }
            }
            dst.save(out)
        }
    }

    fun extractPages(context: Context, uri: Uri, pages: List<Int>, out: File) {
        context.contentResolver.openInputStream(uri)!!.use { input ->
            PDDocument.load(input).use { src ->
                PDDocument().use { dst ->
                    pages.distinct().forEach { oneBased ->
                        val idx = oneBased - 1
                        require(idx in 0 until src.numberOfPages)
                        dst.importPage(src.getPage(idx))
                    }
                    dst.save(out)
                }
            }
        }
    }

    fun imagesToPdf(context: Context, uris: List<Uri>, out: File) {
        val pdf = AndroidPdfDocument()
        try {
            uris.forEachIndexed { index, uri ->
                val bmp = context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) }
                val pageWidth = 1240
                val pageHeight = 1754
                val info = AndroidPdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdf.startPage(info)
                val scale = minOf(pageWidth.toFloat() / bmp.width, pageHeight.toFloat() / bmp.height)
                val w = bmp.width * scale
                val h = bmp.height * scale
                val left = (pageWidth - w) / 2f
                val top = (pageHeight - h) / 2f
                val dst = android.graphics.RectF(left, top, left + w, top + h)
                page.canvas.drawBitmap(bmp, null, dst, null)
                pdf.finishPage(page)
                bmp.recycle()
            }
            FileOutputStream(out).use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
    }
}
