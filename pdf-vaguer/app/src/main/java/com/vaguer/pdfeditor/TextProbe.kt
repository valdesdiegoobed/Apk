package com.vaguer.pdfeditor

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

object TextProbe {
    data class Selection(
        val text: String,
        val x: Float,
        val yTop: Float,
        val width: Float,
        val height: Float,
        val fontSize: Float,
        val fontName: String
    )

    private data class Glyph(
        val text: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val size: Float,
        val fontName: String
    )

    fun nearestWord(document: PDDocument, pageIndex: Int, x: Float, yTop: Float): Selection? {
        val glyphs = mutableListOf<Glyph>()
        val stripper = object : PDFTextStripper() {
            init {
                startPage = pageIndex + 1
                endPage = pageIndex + 1
                sortByPosition = true
            }

            override fun processTextPosition(text: TextPosition) {
                val value = text.unicode ?: ""
                if (value.isNotEmpty()) {
                    glyphs += Glyph(
                        value,
                        text.xDirAdj,
                        text.yDirAdj,
                        text.widthDirAdj.coerceAtLeast(0.5f),
                        text.heightDir.coerceAtLeast(1f),
                        text.fontSizeInPt.coerceAtLeast(1f),
                        text.font?.name ?: "Fuente PDF"
                    )
                }
                super.processTextPosition(text)
            }
        }

        return try {
            stripper.getText(document)
            val candidates = glyphs.filter { it.text.isNotBlank() }
            if (candidates.isEmpty()) return null

            val target = candidates.minByOrNull { g ->
                val cx = g.x + g.width / 2f
                val cy = g.y - g.height / 2f
                hypot((cx - x).toDouble(), (cy - yTop).toDouble()).toFloat()
            } ?: return null

            val distance = hypot(
                (target.x + target.width / 2f - x).toDouble(),
                (target.y - target.height / 2f - yTop).toDouble()
            ).toFloat()
            if (distance > max(55f, target.size * 4f)) return null

            val sameLine = glyphs
                .filter { abs(it.y - target.y) <= max(3.5f, target.size * 0.65f) }
                .sortedBy { it.x }

            val targetIndex = sameLine.indices.minByOrNull { i ->
                abs((sameLine[i].x + sameLine[i].width / 2f) - (target.x + target.width / 2f))
            } ?: return null

            fun joinable(left: Glyph, right: Glyph): Boolean {
                if (left.text.any { it.isWhitespace() } || right.text.any { it.isWhitespace() }) return false
                val gap = right.x - (left.x + left.width)
                val allowance = max(left.size, right.size) * 0.55f
                return gap in (-allowance * 0.35f)..allowance
            }

            var start = targetIndex
            var end = targetIndex
            while (start > 0 && joinable(sameLine[start - 1], sameLine[start])) start--
            while (end + 1 < sameLine.size && joinable(sameLine[end], sameLine[end + 1])) end++

            val word = sameLine.subList(start, end + 1)
            val value = word.joinToString("") { it.text }.trim()
            if (value.isBlank()) return null

            val left = word.minOf { it.x }
            val right = word.maxOf { it.x + it.width }
            val height = word.maxOf { it.height }
            val size = target.size

            Selection(
                text = value,
                x = left,
                yTop = target.y,
                width = (right - left).coerceAtLeast(2f),
                height = height.coerceAtLeast(size * 0.75f),
                fontSize = size,
                fontName = target.fontName
            )
        } catch (_: Exception) {
            null
        }
    }
}
