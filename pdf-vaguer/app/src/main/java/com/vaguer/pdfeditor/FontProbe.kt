package com.vaguer.pdfeditor

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.IOException
import kotlin.math.hypot

object FontProbe {
    data class Match(val font: PDFont, val size: Float, val name: String)

    fun nearest(document: PDDocument, pageIndex: Int, x: Float, yTop: Float): Match? {
        var best: Match? = null
        var bestDistance = Float.MAX_VALUE
        val stripper = object : PDFTextStripper() {
            init {
                startPage = pageIndex + 1
                endPage = pageIndex + 1
                sortByPosition = true
            }

            @Throws(IOException::class)
            override fun processTextPosition(text: TextPosition) {
                val dx = text.xDirAdj - x
                val dy = text.yDirAdj - yTop
                val d = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                if (d < bestDistance && d < 80f) {
                    bestDistance = d
                    best = Match(text.font, text.fontSizeInPt, text.font.name ?: "Fuente PDF")
                }
                super.processTextPosition(text)
            }
        }
        return try {
            stripper.getText(document)
            best
        } catch (_: Exception) {
            null
        }
    }
}
