package com.vaguer.pdfeditor

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
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

    private var zoom = 1f
    private val scaler = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            zoom = (zoom * detector.scaleFactor).coerceIn(1f, 5f)
            scaleX = zoom
            scaleY = zoom
            return true
        }
    })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaler.onTouchEvent(event)
        if (event.pointerCount == 1 && event.action == MotionEvent.ACTION_UP && !scaler.isInProgress && drawable != null) {
            val cx = width / 2f
            val cy = height / 2f
            val unscaledX = (event.x - cx) / zoom + cx
            val unscaledY = (event.y - cy) / zoom + cy
            val inverse = Matrix()
            if (imageMatrix.invert(inverse)) {
                val p = floatArrayOf(unscaledX, unscaledY)
                inverse.mapPoints(p)
                val w = drawable.intrinsicWidth.toFloat().coerceAtLeast(1f)
                val h = drawable.intrinsicHeight.toFloat().coerceAtLeast(1f)
                if (p[0] in 0f..w && p[1] in 0f..h) {
                    normalizedTapX = p[0] / w
                    normalizedTapY = p[1] / h
                    performClick()
                }
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
