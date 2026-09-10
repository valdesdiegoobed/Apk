package com.vaguer.pdfeditor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import android.widget.Magnifier
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.hypot

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
    var onSelectionAdjusted: ((Float, Float, Float, Float) -> Unit)? = null
    var onInkStroke: ((List<Pair<Float, Float>>) -> Unit)? = null

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
    private var handleDrag = 0
    private var magnifier: Magnifier? = null

    private var inkMode = false
    private val inkPoints = mutableListOf<Pair<Float, Float>>()

    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x403B82F6
        style = Paint.Style.FILL
    }
    private val selectionStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF155EEF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF155EEF.toInt()
        style = Paint.Style.FILL
    }
    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCC155EEF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.25f
    }
    private val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF111827.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    init {
        scaleType = ImageView.ScaleType.MATRIX
    }

    private val scaler = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            ensureMatrix()
            return handleDrag == 0 && !inkMode
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (handleDrag != 0 || inkMode) return false
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
            if (selectionMode || handleDrag != 0 || inkMode || userZoom <= 1.001f) return false
            ensureMatrix()
            pageMatrix.postTranslate(-distanceX, -distanceY)
            constrainMatrix()
            applyPageMatrix()
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }
    })

    fun startRangeSelection() {
        inkMode = false
        inkPoints.clear()
        selectionMode = true
        dragStart = null
        handleDrag = 0
        clearSelection()
    }

    fun cancelRangeSelection() {
        selectionMode = false
        dragStart = null
        handleDrag = 0
        dismissMagnifier()
        invalidate()
    }

    fun startInkMode() {
        cancelRangeSelection()
        clearSelection()
        inkPoints.clear()
        inkMode = true
        invalidate()
    }

    fun cancelInkMode() {
        inkMode = false
        inkPoints.clear()
        invalidate()
    }

    fun setSelection(rect: RectF?) {
        selectionRect = rect
        invalidate()
    }

    fun clearSelection() {
        selectionRect = null
        handleDrag = 0
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

    private fun mappedSelectionRect(): RectF? {
        val rect = selectionRect ?: return null
        val d = drawable ?: return null
        val source = RectF(
            rect.left * d.intrinsicWidth,
            rect.top * d.intrinsicHeight,
            rect.right * d.intrinsicWidth,
            rect.bottom * d.intrinsicHeight
        )
        return RectF(source).also { pageMatrix.mapRect(it) }
    }

    private fun detectHandle(x: Float, y: Float): Int {
        val mapped = mappedSelectionRect() ?: return 0
        val radius = 18f * resources.displayMetrics.density
        val cy = mapped.centerY()
        if (hypot((x - mapped.left).toDouble(), (y - cy).toDouble()) <= radius) return 1
        if (hypot((x - mapped.right).toDouble(), (y - cy).toDouble()) <= radius) return 2
        return 0
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
        if (inkMode) {
            if (event.pointerCount > 1) {
                cancelInkMode()
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val p = normalizedPoint(event.x, event.y) ?: return true
                    inkPoints.clear()
                    inkPoints += p
                    normalizedTapX = p.first
                    normalizedTapY = p.second
                    parent?.requestDisallowInterceptTouchEvent(true)
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val p = normalizedPoint(event.x, event.y) ?: return true
                    val last = inkPoints.lastOrNull()
                    if (last == null || hypot((p.first - last.first).toDouble(), (p.second - last.second).toDouble()) > 0.0007) {
                        inkPoints += p
                        invalidate()
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    val p = normalizedPoint(event.x, event.y)
                    if (p != null) inkPoints += p
                    val completed = inkPoints.toList()
                    inkPoints.clear()
                    inkMode = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    invalidate()
                    if (completed.size >= 2) onInkStroke?.invoke(completed)
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    cancelInkMode()
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return true
                }
            }
        }

        if (event.actionMasked == MotionEvent.ACTION_DOWN && !selectionMode && selectionRect != null) {
            val detected = detectHandle(event.x, event.y)
            if (detected != 0) {
                handleDrag = detected
                showMagnifier(event.x, event.y)
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
        }

        if (handleDrag != 0) {
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    val p = normalizedPoint(event.x, event.y) ?: return true
                    val current = selectionRect ?: return true
                    val minGap = 0.0005f
                    selectionRect = if (handleDrag == 1) {
                        RectF(p.first.coerceAtMost(current.right - minGap), current.top, current.right, current.bottom)
                    } else {
                        RectF(current.left, current.top, p.first.coerceAtLeast(current.left + minGap), current.bottom)
                    }
                    normalizedTapX = p.first
                    normalizedTapY = p.second
                    showMagnifier(event.x, event.y)
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    val current = selectionRect
                    dismissMagnifier()
                    handleDrag = 0
                    parent?.requestDisallowInterceptTouchEvent(false)
                    if (current != null) onSelectionAdjusted?.invoke(current.left, current.top, current.right, current.bottom)
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    dismissMagnifier()
                    handleDrag = 0
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return true
                }
            }
        }

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
                    setSelection(RectF(minOf(start.first, p.first), minOf(start.second, p.second), maxOf(start.first, p.first), maxOf(start.second, p.second)))
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

        if (inkPoints.size >= 2) {
            val d = drawable
            if (d != null) {
                val path = Path()
                inkPoints.forEachIndexed { index, p ->
                    val point = floatArrayOf(p.first * d.intrinsicWidth, p.second * d.intrinsicHeight)
                    pageMatrix.mapPoints(point)
                    if (index == 0) path.moveTo(point[0], point[1]) else path.lineTo(point[0], point[1])
                }
                canvas.drawPath(path, inkPaint)
            }
        }

        val mapped = mappedSelectionRect() ?: return
        canvas.drawRoundRect(mapped, 5f, 5f, selectionPaint)
        canvas.drawRoundRect(mapped, 5f, 5f, selectionStroke)
        val r = 6.5f * resources.displayMetrics.density
        val stem = 7f * resources.displayMetrics.density
        canvas.drawLine(mapped.left, mapped.centerY() - stem, mapped.left, mapped.centerY() + stem, selectionStroke)
        canvas.drawLine(mapped.right, mapped.centerY() - stem, mapped.right, mapped.centerY() + stem, selectionStroke)
        canvas.drawCircle(mapped.left, mapped.centerY() + stem, r, handlePaint)
        canvas.drawCircle(mapped.right, mapped.centerY() + stem, r, handlePaint)

        if (selectionMode || handleDrag != 0) {
            canvas.drawLine(mapped.left - 8f, mapped.centerY(), mapped.left + 8f, mapped.centerY(), crosshairPaint)
            canvas.drawLine(mapped.right - 8f, mapped.centerY(), mapped.right + 8f, mapped.centerY(), crosshairPaint)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
