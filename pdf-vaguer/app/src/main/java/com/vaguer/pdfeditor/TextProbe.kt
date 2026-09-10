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
    ) {
        val left get() = x
        val right get() = x + width
        val top get() = y - height
        val bottom get() = y
        val centerX get() = x + width / 2f
        val centerY get() = y - height / 2f
    }

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
                        text.widthDirAdj.coerceAtLeast(0.5f),
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
            val glyphs = collectGlyphs(document, pageIndex).filter { it.text.isNotBlank() }
            if (glyphs.isEmpty()) return null

            val containing = glyphs.filter { g ->
                val padX = max(1.5f, g.size * 0.12f)
                val padY = max(1.5f, g.size * 0.18f)
                x in (g.left - padX)..(g.right + padX) && yTop in (g.top - padY)..(g.bottom + padY)
            }

            val target = (containing.minByOrNull { abs(it.centerX - x) + abs(it.centerY - yTop) }
                ?: glyphs.minByOrNull { g ->
                    hypot((g.centerX - x).toDouble(), (g.centerY - yTop).toDouble()).toFloat()
                }) ?: return null

            if (containing.isEmpty()) {
                val distance = hypot((target.centerX - x).toDouble(), (target.centerY - yTop).toDouble()).toFloat()
                val limit = max(10f, target.size * 1.45f)
                if (distance > limit) return null
            }

            val lineTolerance = max(2.5f, target.size * 0.38f)
            val sameLine = glyphs
                .filter { abs(it.y - target.y) <= lineTolerance }
                .sortedBy { it.x }

            val targetIndex = sameLine.indices.minByOrNull { i -> abs(sameLine[i].centerX - target.centerX) } ?: return null

            fun joinable(left: Glyph, right: Glyph): Boolean {
                if (left.text.any { it.isWhitespace() } || right.text.any { it.isWhitespace() }) return false
                if (left.fontName != right.fontName) return false
                if (abs(left.size - right.size) > max(0.5f, left.size * 0.08f)) return false
                val gap = right.x - left.right
                val allowance = max(left.size, right.size) * 0.42f
                return gap in (-allowance * 0.20f)..allowance
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
            val yMid = (y1 + y2) / 2f

            val lines = mutableListOf<MutableList<Glyph>>()
            glyphs.sortedBy { it.y }.forEach { g ->
                val line = lines.firstOrNull { existing ->
                    val avgY = existing.map { it.y }.average().toFloat()
                    abs(avgY - g.y) <= max(2.5f, g.size * 0.40f)
                }
                if (line != null) line += g else lines += mutableListOf(g)
            }

            val lineCandidates = lines.mapNotNull { line ->
                val horizontallyRelevant = line.filter { g -> g.right >= x1 && g.left <= x2 }
                if (horizontallyRelevant.isEmpty()) null
                else {
                    val avgY = line.map { it.y }.average().toFloat()
                    val avgH = line.map { it.height }.average().toFloat()
                    val centerY = avgY - avgH / 2f
                    Triple(line, horizontallyRelevant, abs(centerY - yMid))
                }
            }
            if (lineCandidates.isEmpty()) return null

            val chosen = lineCandidates.minByOrNull { it.third } ?: return null
            val line = chosen.first
            val rough = chosen.second
            val verticalDistance = chosen.third
            val typicalSize = rough.map { it.size }.average().toFloat().coerceAtLeast(1f)
            if (verticalDistance > max(10f, typicalSize * 1.35f)) return null

            val anchor = rough.minByOrNull { abs(it.centerX - (x1 + x2) / 2f) } ?: return null
            val dominantFont = rough.groupBy { it.fontName }
                .maxByOrNull { (_, list) -> list.sumOf { it.text.length } }
                ?.key ?: anchor.fontName
            val dominantSize = rough.filter { it.fontName == dominantFont }
                .map { it.size }
                .average()
                .toFloat()
                .takeIf { !it.isNaN() } ?: anchor.size

            val selected = line.filter { g ->
                val horizontalOverlap = g.right >= x1 && g.left <= x2
                horizontalOverlap &&
                    g.fontName == dominantFont &&
                    abs(g.size - dominantSize) <= max(0.45f, dominantSize * 0.08f)
            }.sortedBy { it.x }
            if (selected.isEmpty()) return null

            buildSelection(selected, selected.first())
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
                val gap = g.x - prev.right
                val spaceThreshold = max(prev.size, g.size) * 0.26f
                if (gap > spaceThreshold && !sb.endsWith(" ")) sb.append(' ')
            }
            sb.append(g.text)
        }
        val value = sb.toString().trim()
        if (value.isBlank()) return null

        val left = sorted.minOf { it.left }
        val right = sorted.maxOf { it.right }
        val top = sorted.minOf { it.top }
        val bottom = sorted.maxOf { it.bottom }
        val baseline = sorted.map { it.y }.average().toFloat()
        val avgSize = sorted.map { it.size }.average().toFloat()

        return Selection(
            text = value,
            x = left,
            yTop = baseline,
            width = (right - left).coerceAtLeast(1f),
            height = (bottom - top).coerceAtLeast(avgSize * 0.72f),
            fontSize = avgSize,
            fontName = target.fontName
        )
    }
}
