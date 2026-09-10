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

    private fun collectGlyphs(document: PDDocument, pageIndex: Int): List<Glyph> {
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
                        text.widthDirAdj.coerceAtLeast(0.25f),
                        text.heightDir.coerceAtLeast(1f),
                        text.fontSizeInPt.coerceAtLeast(1f),
                        text.font?.name ?: "Fuente PDF"
                    )
                }
                super.processTextPosition(text)
            }
        }
        stripper.getText(document)
        return glyphs
    }

    fun nearestWord(document: PDDocument, pageIndex: Int, x: Float, yTop: Float): Selection? {
        return try {
            val glyphs = collectGlyphs(document, pageIndex)
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
            if (distance > max(22f, target.size * 2.0f)) return null

            val lineTolerance = max(1.8f, target.size * 0.30f)
            val sameLine = glyphs
                .filter { abs(it.y - target.y) <= lineTolerance }
                .sortedBy { it.x }

            val targetIndex = sameLine.indices.minByOrNull { i ->
                abs((sameLine[i].x + sameLine[i].width / 2f) - (target.x + target.width / 2f))
            } ?: return null

            fun sameStyle(a: Glyph, b: Glyph): Boolean {
                return a.fontName == b.fontName && abs(a.size - b.size) <= max(0.45f, a.size * 0.08f)
            }

            fun joinable(left: Glyph, right: Glyph): Boolean {
                if (!sameStyle(left, right)) return false
                if (left.text.any { it.isWhitespace() } || right.text.any { it.isWhitespace() }) return false
                val gap = right.x - (left.x + left.width)
                val allowance = max(left.size, right.size) * 0.42f
                return gap in (-allowance * 0.25f)..allowance
            }

            var start = targetIndex
            var end = targetIndex
            while (start > 0 && joinable(sameLine[start - 1], sameLine[start])) start--
            while (end + 1 < sameLine.size && joinable(sameLine[end], sameLine[end + 1])) end++

            buildSelection(sameLine.subList(start, end + 1), target)
        } catch (_: Exception) {
            null
        }
    }

    fun selectRange(
        document: PDDocument,
        pageIndex: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ): Selection? {
        return try {
            val glyphs = collectGlyphs(document, pageIndex).filter { it.text.isNotBlank() }
            if (glyphs.isEmpty()) return null

            val x1 = minOf(left, right)
            val x2 = maxOf(left, right)
            val y1 = minOf(top, bottom)
            val y2 = maxOf(top, bottom)
            val targetY = (y1 + y2) / 2f
            val targetX = (x1 + x2) / 2f

            val anchor = glyphs.minByOrNull { g ->
                val cy = g.y - g.height / 2f
                val vertical = abs(cy - targetY)
                val horizontal = when {
                    targetX < g.x -> g.x - targetX
                    targetX > g.x + g.width -> targetX - (g.x + g.width)
                    else -> 0f
                }
                vertical * 3f + horizontal * 0.12f
            } ?: return null

            val lineTolerance = max(1.8f, anchor.size * 0.30f)
            val sameLine = glyphs
                .filter { abs(it.y - anchor.y) <= lineTolerance }
                .sortedBy { it.x }
            if (sameLine.isEmpty()) return null

            val rough = sameLine.filter { g ->
                val gx1 = g.x
                val gx2 = g.x + g.width
                val overlap = minOf(gx2, x2) - maxOf(gx1, x1)
                val ratio = overlap / g.width.coerceAtLeast(0.25f)
                ratio >= 0.42f || (g.x + g.width / 2f) in x1..x2
            }
            if (rough.isEmpty()) return null

            val dominantFont = rough.groupBy { it.fontName }
                .maxByOrNull { (_, list) -> list.sumOf { it.text.length.coerceAtLeast(1) } }
                ?.key ?: anchor.fontName
            val dominantSize = rough.filter { it.fontName == dominantFont }
                .map { it.size }
                .average().toFloat().takeIf { !it.isNaN() } ?: anchor.size

            val formattedLine = sameLine.filter {
                it.fontName == dominantFont && abs(it.size - dominantSize) <= max(0.45f, dominantSize * 0.08f)
            }.sortedBy { it.x }
            if (formattedLine.isEmpty()) return null

            val selectedIndices = formattedLine.indices.filter { i ->
                val g = formattedLine[i]
                val gx1 = g.x
                val gx2 = g.x + g.width
                val overlap = minOf(gx2, x2) - maxOf(gx1, x1)
                val ratio = overlap / g.width.coerceAtLeast(0.25f)
                ratio >= 0.42f || (g.x + g.width / 2f) in x1..x2
            }
            if (selectedIndices.isEmpty()) return null

            val first = selectedIndices.first()
            val last = selectedIndices.last()
            buildSelection(formattedLine.subList(first, last + 1), formattedLine[first])
        } catch (_: Exception) {
            null
        }
    }

    private fun buildSelection(items: List<Glyph>, target: Glyph): Selection? {
        if (items.isEmpty()) return null
        val sorted = items.sortedBy { it.x }
        val sb = StringBuilder()
        sorted.forEachIndexed { index, g ->
            if (index > 0) {
                val prev = sorted[index - 1]
                val gap = g.x - (prev.x + prev.width)
                val spaceThreshold = max(prev.size, g.size) * 0.24f
                if (gap > spaceThreshold && !sb.endsWith(" ")) sb.append(' ')
            }
            sb.append(g.text)
        }
        val value = sb.toString().trim()
        if (value.isBlank()) return null

        val left = sorted.minOf { it.x }
        val right = sorted.maxOf { it.x + it.width }
        val height = sorted.maxOf { it.height }
        val baseline = sorted.map { it.y }.average().toFloat()
        val avgSize = sorted.map { it.size }.average().toFloat()

        return Selection(
            text = value,
            x = left,
            yTop = baseline,
            width = (right - left).coerceAtLeast(1f),
            height = height.coerceAtLeast(avgSize * 0.72f),
            fontSize = avgSize,
            fontName = target.fontName
        )
    }
}
