package com.fullbody.season.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/** Простой линейный график рабочего веса по датам. */
class ChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    /** Точки: (подпись даты, значение). */
    var points: List<Pair<String, Float>> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF7A00")
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF7A00")
        style = Paint.Style.FILL
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#262B36")
        strokeWidth = 2f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9AA3B2")
        textSize = 28f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val padL = 90f
        val padR = 30f
        val padT = 40f
        val padB = 70f
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= padL + padR || h <= padT + padB) return

        // Сетка
        for (i in 0..3) {
            val y = padT + (h - padT - padB) * i / 3f
            canvas.drawLine(padL, y, w - padR, y, gridPaint)
        }

        val pts = points
        if (pts.size < 2) {
            if (pts.size == 1) {
                canvas.drawCircle((padL + w - padR) / 2, (padT + h - padB) / 2, 8f, dotPaint)
                canvas.drawText(
                    formatWeight(pts[0].second), padL, padT + 20f, textPaint
                )
            }
            return
        }

        val min = pts.minOf { it.second }
        val max = pts.maxOf { it.second }
        val span = if (max - min < 0.01f) 1f else max - min

        fun x(i: Int) = padL + (w - padL - padR) * i / (pts.size - 1).toFloat()
        fun y(v: Float) = padT + (h - padT - padB) * (1f - (v - min) / span)

        canvas.drawText(formatWeight(max), 8f, padT + 10f, textPaint)
        canvas.drawText(formatWeight(min), 8f, h - padB, textPaint)

        val path = Path()
        pts.forEachIndexed { i, p ->
            if (i == 0) path.moveTo(x(i), y(p.second)) else path.lineTo(x(i), y(p.second))
        }
        canvas.drawPath(path, linePaint)
        pts.forEachIndexed { i, p ->
            canvas.drawCircle(x(i), y(p.second), 7f, dotPaint)
        }

        // Подписи первой и последней даты
        canvas.drawText(pts.first().first, padL, h - padB + 45f, textPaint)
        val last = pts.last().first
        canvas.drawText(last, w - padR - textPaint.measureText(last), h - padB + 45f, textPaint)
    }

    private fun formatWeight(v: Float): String =
        if (v % 1f == 0f) "${v.toInt()} кг" else "%.1f кг".format(v).replace(',', '.')
}
