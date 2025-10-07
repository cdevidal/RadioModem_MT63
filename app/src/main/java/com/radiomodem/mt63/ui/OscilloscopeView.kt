package com.radiomodem.mt63.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

class OscilloscopeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        alpha = 80
    }

    @Volatile private var data: ShortArray = ShortArray(0)

    fun update(samples: ShortArray) {
        data = samples.copyOf()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        // grid
        val mid = h/2f
        canvas.drawLine(0f, mid, w, mid, grid)
        val n = data.size
        if (n < 2) return
        var lastX = 0f
        var lastY = mid
        for (i in 0 until n) {
            val x = i * (w / (n - 1).toFloat())
            val y = mid - (data[i] / 32768f) * (h * 0.45f)
            if (i > 0) canvas.drawLine(lastX, lastY, x, y, paint)
            lastX = x; lastY = y
        }
    }
}