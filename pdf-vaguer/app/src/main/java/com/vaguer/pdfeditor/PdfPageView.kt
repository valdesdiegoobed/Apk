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

    private var zoom = 1f
    private var selectionRect: RectF? = null
    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x6633A1FF
        style = Paint.Style.FILL
    }

    private val scaler = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
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

    fun setSelection(rect: RectF?) {
        selectionRect = rect
        invalidate()
    }

    fun clearSelection() = setSelection(null)

    private fun updateTap(x: Float, y: Float): Boolean {
        val d = drawable ?: return false
        val cx = width / 2f
        val cy = height / 2f
        val unscaledX = (x - cx) / zoom + cx
        val unscaledY = (y - cy) / zoom + cy
        val inverse = Matrix()
        if (!imageMatrix.invert(inverse)) return false
        val p = floatArrayOf(unscaledX, unscaledY)
        inverse.mapPoints(p)
        val w = d.intrinsicWidth.toFloat().coerceAtLeast(1f)
        val h = d.intrinsicHeight.toFloat().coerceAtLeast(1f)
        if (p[0] !in 0f..w || p[1] !in 0f..h) return false
        normalizedTapX = p[0] / w
        normalizedTapY = p[1] / h
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
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
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
