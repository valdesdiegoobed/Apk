package com.vaguer.pdfeditor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import android.widget.Magnifier
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

    private val pageMatrix = Matrix()
    private var matrixReady = false
    private var lastDrawableWidth = 0
    private var lastDrawableHeight = 0
    private var userZoom = 1f
    private val minZoom = 1f
    private val maxZoom = 10f

    private var selectionRect: RectF? = null
    private var selectionMode = false
    private var dragStart: Pair<Float, Float>? = null
    private var magnifier: Magnifier? = null

    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x4433A1FF
        style = Paint.Style.FILL
    }
    private val selectionStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1976D2.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1976D2.toInt()
        style = Paint.Style.FILL
    }
    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCC1976D2.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    init {
        scaleType = ImageView.ScaleType.MATRIX
    }

    private val scaler = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            ensureMatrix()
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            ensureMatrix()
            val next = (userZoom * detector.scaleFactor).coerceIn(minZoom, maxZoom)
            val actual = next / userZoom
            if (actual != 1f) {
                pageMatrix.postScale(actual, actual, detector.focusX, detector.focusY)
                userZoom = next
                constrainMatrix()
                applyPageMatrix()
            }
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

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (selectionMode || userZoom <= 1.001f) return false
            ensureMatrix()
            pageMatrix.postTranslate(-distanceX, -distanceY)
            constrainMatrix()
            applyPageMatrix()
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
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
        dismissMagnifier()
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

    fun resetZoom() {
        matrixReady = false
        userZoom = 1f
        ensureMatrix()
        invalidate()
    }

    private fun ensureMatrix() {
        val d = drawable ?: return
        if (width <= 0 || height <= 0) return
        val dw = d.intrinsicWidth.coerceAtLeast(1)
        val dh = d.intrinsicHeight.coerceAtLeast(1)
        if (matrixReady && dw == lastDrawableWidth && dh == lastDrawableHeight) return

        lastDrawableWidth = dw
        lastDrawableHeight = dh
        userZoom = 1f
        val src = RectF(0f, 0f, dw.toFloat(), dh.toFloat())
        val dst = RectF(0f, 0f, width.toFloat(), height.toFloat())
        pageMatrix.reset()
        pageMatrix.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER)
        matrixReady = true
        applyPageMatrix()
    }

    private fun applyPageMatrix() {
        imageMatrix = pageMatrix
        invalidate()
    }

    private fun constrainMatrix() {
        val d = drawable ?: return
        val rect = RectF(0f, 0f, d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
        pageMatrix.mapRect(rect)
        var dx = 0f
        var dy = 0f

        if (rect.width() <= width) {
            dx = width / 2f - rect.centerX()
        } else {
            if (rect.left > 0f) dx = -rect.left
            else if (rect.right < width) dx = width - rect.right
        }

        if (rect.height() <= height) {
            dy = height / 2f - rect.centerY()
        } else {
            if (rect.top > 0f) dy = -rect.top
            else if (rect.bottom < height) dy = height - rect.bottom
        }

        if (dx != 0f || dy != 0f) pageMatrix.postTranslate(dx, dy)
    }

    private fun normalizedPoint(x: Float, y: Float): Pair<Float, Float>? {
        val d = drawable ?: return null
        ensureMatrix()
        val inverse = Matrix()
        if (!pageMatrix.invert(inverse)) return null
        val p = floatArrayOf(x, y)
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

    private fun showMagnifier(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (magnifier == null) magnifier = Magnifier(this)
            runCatching { magnifier?.show(x, y) }
        }
    }

    private fun dismissMagnifier() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { magnifier?.dismiss() }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        matrixReady = false
        ensureMatrix()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaler.onTouchEvent(event)

        if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN || event.pointerCount > 1 || scaler.isInProgress) {
            if (selectionMode) {
                dragStart = null
                clearSelection()
                dismissMagnifier()
            }
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }

        if (selectionMode && event.pointerCount == 1) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val p = normalizedPoint(event.x, event.y) ?: return true
                    dragStart = p
                    normalizedTapX = p.first
                    normalizedTapY = p.second
                    setSelection(RectF(p.first, p.second, p.first, p.second))
                    showMagnifier(event.x, event.y)
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
                    showMagnifier(event.x, event.y)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    val start = dragStart
                    val p = normalizedPoint(event.x, event.y)
                    selectionMode = false
                    dragStart = null
                    dismissMagnifier()
                    parent?.requestDisallowInterceptTouchEvent(false)
                    if (start != null && p != null) {
                        val left = minOf(start.first, p.first)
                        val right = maxOf(start.first, p.first)
                        val top = minOf(start.second, p.second)
                        val bottom = maxOf(start.second, p.second)
                        if ((right - left) > 0.0015f || (bottom - top) > 0.0015f) {
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

        gestures.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            parent?.requestDisallowInterceptTouchEvent(false)
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        ensureMatrix()
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
        pageMatrix.mapRect(mapped)
        canvas.drawRoundRect(mapped, 4f, 4f, selectionPaint)
        canvas.drawRoundRect(mapped, 4f, 4f, selectionStroke)
        val r = 7f
        canvas.drawCircle(mapped.left, mapped.centerY(), r, handlePaint)
        canvas.drawCircle(mapped.right, mapped.centerY(), r, handlePaint)

        if (selectionMode) {
            canvas.drawLine(mapped.left - 10f, mapped.centerY(), mapped.left + 10f, mapped.centerY(), crosshairPaint)
            canvas.drawLine(mapped.left, mapped.centerY() - 10f, mapped.left, mapped.centerY() + 10f, crosshairPaint)
            canvas.drawLine(mapped.right - 10f, mapped.centerY(), mapped.right + 10f, mapped.centerY(), crosshairPaint)
            canvas.drawLine(mapped.right, mapped.centerY() - 10f, mapped.right, mapped.centerY() + 10f, crosshairPaint)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
