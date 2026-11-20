package com.dinalamin.lifeblood.ui.util

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * A heartbeat (ECG) line that continuously scrolls from right to left.
 * The ends fade out so the wave appears to emerge from and vanish into the header.
 */
class EcgLineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val periodPx = 96f * density
    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.7f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
    }

    // Shape of one heartbeat: x as a fraction of the period, y as a fraction of half-height (negative = up).
    private val beat = arrayOf(
        0.00f to 0f, 0.28f to 0f,
        0.33f to -0.18f, 0.38f to 0f,
        0.46f to 0f, 0.50f to 0.22f,
        0.56f to -1.00f, 0.62f to 0.55f,
        0.67f to 0f, 0.76f to 0f,
        0.82f to -0.28f, 0.88f to 0f,
        1.00f to 0f
    )

    private var phase = 0f
    private var animator: ValueAnimator? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val white = Color.WHITE
        val clear = Color.argb(0, 255, 255, 255)
        val soft = Color.argb(200, 255, 255, 255)
        paint.shader = LinearGradient(
            0f, 0f, w.toFloat(), 0f,
            intArrayOf(clear, soft, white, soft, clear),
            floatArrayOf(0f, 0.18f, 0.5f, 0.82f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val mid = height / 2f
        val amp = height / 2f - paint.strokeWidth
        path.reset()
        var started = false
        var base = -periodPx - phase
        while (base < w + periodPx) {
            for ((fx, fy) in beat) {
                val x = base + fx * periodPx
                val y = mid + fy * amp
                if (!started) { path.moveTo(x, y); started = true } else path.lineTo(x, y)
            }
            base += periodPx
        }
        canvas.drawPath(path, paint)
    }

    private fun start() {
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, periodPx).apply {
            duration = 2200
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { phase = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    private fun stop() {
        animator?.cancel()
        animator = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        start()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) start() else stop()
    }
}
