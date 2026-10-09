package ai.flux.mobile.assistant

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import kotlin.math.sin
import kotlin.math.cos
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class FluxEnergyOrbView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var pulse = 0f
    private var active = false
    private var audioLevel=0f
    fun setAudioLevel(value:Float){audioLevel=value.coerceIn(0f,1f);invalidate()}
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
        paint.shader = RadialGradient(cx,cy,base*1.3f,intArrayOf(Color.argb(45,Color.red(accent),Color.green(accent),Color.blue(accent)),Color.TRANSPARENT),null,Shader.TileMode.CLAMP)
        canvas.drawCircle(cx,cy,base*1.3f,paint)
        paint.shader=null
        paint.style=Paint.Style.STROKE
        repeat(8){line->
            val path=Path()
            for(step in 0..120){
                val angle=step/120f*2f*Math.PI.toFloat()
                val radius=base+sin(angle*(2.7f+line*.11f)+line*.86f+pulse)*base*(.08f+audioLevel*.10f)
                val x=cx+cos(angle)*radius;val y=cy+sin(angle)*radius*(.84f+line*.015f)
                if(step==0)path.moveTo(x,y)else path.lineTo(x,y)
            }
            paint.color=Color.argb(80+line*18,Color.red(accent),Color.green(accent),Color.blue(accent));paint.strokeWidth=resources.displayMetrics.density*(if(line%3==0)1.4f else .8f)
            canvas.drawPath(path,paint)
        }
        paint.style=Paint.Style.FILL
        paint.shader = null
    }

    private fun accentColor(): Int = when (
        context.getSharedPreferences("flux_workspace", Context.MODE_PRIVATE)
            .getString("accent_key", "blue")
    ) {
        "blue" -> Color.rgb(0, 183, 255)
        "cyan" -> Color.rgb(0, 229, 255)
        "purple" -> Color.rgb(123, 97, 255)
        "gold" -> Color.rgb(255, 184, 64)
        else -> Color.rgb(0, 183, 255)
    }

    private fun darken(color: Int): Int = Color.rgb(
        (Color.red(color) * .42f).toInt(),
        (Color.green(color) * .42f).toInt(),
        (Color.blue(color) * .42f).toInt(),
    )
}
