package com.nerigon.cumculator

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.util.Calendar
import kotlin.math.min

class CircularTimerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        strokeWidth = 25f
        style = Paint.Style.STROKE
    }

    private val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN
        strokeWidth = 25f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 70f
        textAlign = Paint.Align.CENTER
    }

    private val startTime = Calendar.getInstance().apply {
        set(2024, Calendar.APRIL, 12, 16, 0, 0)
    }.timeInMillis

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val now = System.currentTimeMillis()
        val elapsedMillis = now - startTime
        val totalSeconds = elapsedMillis / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = (totalSeconds / 3600)

        val text = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(centerX, centerY) - 40f
        val oval = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

        canvas.drawArc(oval, 0f, 360f, false, bgPaint)

        val progress = (totalSeconds % 60) * 6f
        canvas.drawArc(oval, -90f, progress, false, fgPaint)

        canvas.drawText(text, centerX, centerY + 25f, textPaint)

        postInvalidateDelayed(1000)
    }
}
