package com.summitcodeworks.imager.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class NeonGradientView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paint = Paint()
    private var lastX = 0f
    private var lastY = 0f
    private var dX = 0f
    private var dY = 0f

    init {
        // Optional: Enable hardware acceleration for smoother performance
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // Create a linear gradient
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Record the initial touch position
                lastX = event.rawX
                lastY = event.rawY
                dX = x - lastX
                dY = y - lastY
            }
            MotionEvent.ACTION_MOVE -> {
                // Update the view's position as the user drags
                val newX = event.rawX + dX
                val newY = event.rawY + dY

                // Set the new position
                x = newX
                y = newY
            }
        }
        return true
    }
}
