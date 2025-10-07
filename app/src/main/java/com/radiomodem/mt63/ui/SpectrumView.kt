package com.radiomodem.mt63.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class SpectrumView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val bar = Paint(Paint.ANTI_ALIAS_FLAG)
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 80 }
    @Volatile private var data: FloatArray = FloatArray(0)

    fun update(magnitudes: FloatArray) {
        data = magnitudes.copyOf()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val n = data.size
        if (n == 0) return
        // grid midline
        canvas.drawLine(0f, h*0.75f, w, h*0.75f, grid)
        val bw = w / n
        for (i in 0 until n) {
            val v = data[i].coerceIn(0f, 1f)
            val top = h - v * (h*0.7f)
            canvas.drawRect(i*bw, top, (i+1)*bw*0.98f, h, bar)
        }
    }
}