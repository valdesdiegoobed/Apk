package com.vaguer.pdfeditor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

class PdfPageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    var normalizedTapX: Float? = null
        private set
    var normalizedTapY: Float? = null
        private set

    var onSingleTapPositioned: (() -> Unit)? = null
    var onTextSelectionGesture: (() -> Unit)? = null
    var onRangeSelected: ((Float, Float, Float, Float) -> Unit)? = null

    private var zoom = 1f
    private var selectionRect: RectF? = null
    private var selectionMode = false
    private var dragStart: Pair<Float, Float>? = null

    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x5533A1FF
        style = Paint.Style.FILL
    }
    private val selectionStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1976D2.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1976D2.toInt()
        style = Paint.Style.FILL
    }

    private val scaler = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (selectionMode) return false
            zoom = (zoom * detector.scaleFactor).coerceIn(1f, 5f)
            scaleX = zoom
            scaleY = zoom
            return true
        }
    })

    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (updateTap(e.x, e.y)) {
                onSingleTapPositioned?.invoke()
                performClick()
            }
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (updateTap(e.x, e.y)) onTextSelectionGesture?.invoke()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            if (updateTap(e.x, e.y)) onTextSelectionGesture?.invoke()
        }
    })

    fun startRangeSelection() {
        selectionMode = true
        dragStart = null
        clearSelection()
    }

    fun cancelRangeSelection() {
        selectionMode = false
        dragStart = null
        invalidate()
    }

    fun setSelection(rect: RectF?) {
        selectionRect = rect
        invalidate()
    }

    fun clearSelection() {
        selectionRect = null
        invalidate()
    }

    private fun normalizedPoint(x: Float, y: Float): Pair<Float, Float>? {
        val d = drawable ?: return null
        val cx = width / 2f
        val cy = height / 2f
        val unscaledX = (x - cx) / zoom + cx
        val unscaledY = (y - cy) / zoom + cy
        val inverse = Matrix()
        if (!imageMatrix.invert(inverse)) return null
        val p = floatArrayOf(unscaledX, unscaledY)
        inverse.mapPoints(p)
        val w = d.intrinsicWidth.toFloat().coerceAtLeast(1f)
        val h = d.intrinsicHeight.toFloat().coerceAtLeast(1f)
        if (p[0] !in 0f..w || p[1] !in 0f..h) return null
        return Pair((p[0] / w).coerceIn(0f, 1f), (p[1] / h).coerceIn(0f, 1f))
    }

    private fun updateTap(x: Float, y: Float): Boolean {
        val p = normalizedPoint(x, y) ?: return false
        normalizedTapX = p.first
        normalizedTapY = p.second
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (selectionMode && event.pointerCount == 1) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val p = normalizedPoint(event.x, event.y) ?: return true
                    dragStart = p
                    normalizedTapX = p.first
                    normalizedTapY = p.second
                    setSelection(RectF(p.first, p.second, p.first, p.second))
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val start = dragStart ?: return true
                    val p = normalizedPoint(event.x, event.y) ?: return true
                    val left = minOf(start.first, p.first)
                    val right = maxOf(start.first, p.first)
                    val top = minOf(start.second, p.second)
                    val bottom = maxOf(start.second, p.second)
                    setSelection(RectF(left, top, right, bottom))
                    normalizedTapX = p.first
                    normalizedTapY = p.second
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    val start = dragStart
                    val p = normalizedPoint(event.x, event.y)
                    selectionMode = false
                    dragStart = null
                    parent?.requestDisallowInterceptTouchEvent(false)
                    if (start != null && p != null) {
                        val left = minOf(start.first, p.first)
                        val right = maxOf(start.first, p.first)
                        val top = minOf(start.second, p.second)
                        val bottom = maxOf(start.second, p.second)
                        if ((right - left) > 0.003f || (bottom - top) > 0.003f) {
                            onRangeSelected?.invoke(left, top, right, bottom)
                        } else {
                            normalizedTapX = p.first
                            normalizedTapY = p.second
                            onTextSelectionGesture?.invoke()
                        }
                    }
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    cancelRangeSelection()
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return true
                }
            }
        }

        scaler.onTouchEvent(event)
        if (!scaler.isInProgress && event.pointerCount == 1) gestures.onTouchEvent(event)
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rect = selectionRect ?: return
        val d = drawable ?: return
        val source = RectF(
            rect.left * d.intrinsicWidth,
            rect.top * d.intrinsicHeight,
            rect.right * d.intrinsicWidth,
            rect.bottom * d.intrinsicHeight
        )
        val mapped = RectF(source)
        imageMatrix.mapRect(mapped)
        canvas.drawRoundRect(mapped, 5f, 5f, selectionPaint)
        canvas.drawRoundRect(mapped, 5f, 5f, selectionStroke)
        val r = 8f
        canvas.drawCircle(mapped.left, mapped.top, r, handlePaint)
        canvas.drawCircle(mapped.right, mapped.bottom, r, handlePaint)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
