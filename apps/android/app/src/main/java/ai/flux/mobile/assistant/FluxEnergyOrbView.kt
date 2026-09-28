package ai.flux.mobile.assistant

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class FluxEnergyOrbView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var pulse = 0f
    private var active = false
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1500L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            pulse = it.animatedValue as Float
            invalidate()
        }
    }

    fun setActive(value: Boolean) {
        active = value
        if (!animator.isStarted) animator.start()
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.isStarted) animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val base = minOf(width, height) * 0.30f
        val accent = accentColor()
        paint.shader = null
        paint.color = Color.argb((35 + pulse * 35).toInt(), Color.red(accent), Color.green(accent), Color.blue(accent))
        canvas.drawCircle(cx, cy, base * (1.45f + pulse * 0.15f), paint)
        paint.shader = RadialGradient(
            cx, cy, base,
            intArrayOf(Color.WHITE, accent, darken(accent), Color.TRANSPARENT),
            floatArrayOf(0f, .22f, .68f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, base * if (active) 1.08f else 0.96f, paint)
        paint.shader = null
    }

    private fun accentColor(): Int = when (
        context.getSharedPreferences("flux_workspace", Context.MODE_PRIVATE)
            .getString("accent_key", "red")
    ) {
        "cyan" -> Color.rgb(0, 229, 255)
        "purple" -> Color.rgb(123, 97, 255)
        "gold" -> Color.rgb(255, 184, 64)
        else -> Color.rgb(255, 48, 74)
    }

    private fun darken(color: Int): Int = Color.rgb(
        (Color.red(color) * .42f).toInt(),
        (Color.green(color) * .42f).toInt(),
        (Color.blue(color) * .42f).toInt(),
    )
}
