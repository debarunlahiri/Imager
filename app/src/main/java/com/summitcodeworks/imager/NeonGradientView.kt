package com.summitcodeworks.imager

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class NeonGradientView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paint = Paint()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Create a neon gradient
        val width = width.toFloat()
        val height = height.toFloat()

        val gradient = LinearGradient(
            0f, 0f, width, height,
            intArrayOf(
                0xFF000000.toInt(), // Black
                0xFF800080.toInt(), // Purple
                0xFF00FFFF.toInt()  // Cyan
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        paint.shader = gradient
        canvas.drawRect(0f, 0f, width, height, paint)
    }
}
