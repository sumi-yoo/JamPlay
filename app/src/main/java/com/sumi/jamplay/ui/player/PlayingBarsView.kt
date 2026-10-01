package com.sumi.jamplay.ui.player

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.abs
import kotlin.math.sin

class PlayingBarsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private var phase = 0f
    var isPlaying = false
        set(value) {
            field = value
            updateAnimation()
        }
    private val animator = ValueAnimator.ofFloat(0f, (Math.PI * 2).toFloat()).apply {
        duration = 1200
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    private fun updateAnimation() {
        if (isPlaying && isAttachedToWindow && isShown && windowVisibility == VISIBLE) {
            if (!animator.isStarted) animator.start()
        } else {
            animator.cancel()
            phase = 0f
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAnimation()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) updateAnimation() else animator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val barWidth = 3 * density
        val gap = 3 * density
        val start = (width - (barWidth * 5 + gap * 4)) / 2
        repeat(5) { index ->
            val barHeight = (6 + if (isPlaying) 18 * abs(sin(phase + index)) else 0f) * density
            val left = start + index * (barWidth + gap)
            canvas.drawRoundRect(left, (height - barHeight) / 2, left + barWidth, (height + barHeight) / 2, density, density, paint)
        }
    }
}
