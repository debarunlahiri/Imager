package com.summitcodeworks.imager

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var cropRect = RectF()
    private var handleRadius = 20f
    private var selectedHandle: Int = HANDLE_NONE
    private val paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val handlePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#80000000")
        style = Paint.Style.FILL
    }

    var onCropChangeListener: ((RectF) -> Unit)? = null

    companion object {
        private const val HANDLE_NONE = -1
        private const val HANDLE_TOP_LEFT = 0
        private const val HANDLE_TOP_RIGHT = 1
        private const val HANDLE_BOTTOM_LEFT = 2
        private const val HANDLE_BOTTOM_RIGHT = 3
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (cropRect.isEmpty) {
            // Initialize crop rect to full view size with 10% margin
            val margin = min(w, h) * 0.1f
            cropRect.set(
                margin,
                margin,
                w - margin,
                h - margin
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw semi-transparent overlay
        canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, overlayPaint)
        canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), overlayPaint)
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, overlayPaint)
        canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, overlayPaint)

        // Draw crop rectangle
        canvas.drawRect(cropRect, paint)

        // Draw handles
        canvas.drawCircle(cropRect.left, cropRect.top, handleRadius, handlePaint)
        canvas.drawCircle(cropRect.right, cropRect.top, handleRadius, handlePaint)
        canvas.drawCircle(cropRect.left, cropRect.bottom, handleRadius, handlePaint)
        canvas.drawCircle(cropRect.right, cropRect.bottom, handleRadius, handlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedHandle = getHandleAtPoint(x, y)
                return selectedHandle != HANDLE_NONE
            }
            MotionEvent.ACTION_MOVE -> {
                if (selectedHandle != HANDLE_NONE) {
                    updateCropRect(x, y)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (selectedHandle != HANDLE_NONE) {
                    selectedHandle = HANDLE_NONE
                    onCropChangeListener?.invoke(cropRect)
                    return true
                }
            }
        }
        return false
    }

    private fun getHandleAtPoint(x: Float, y: Float): Int {
        return when {
            isInHandle(x, y, cropRect.left, cropRect.top) -> HANDLE_TOP_LEFT
            isInHandle(x, y, cropRect.right, cropRect.top) -> HANDLE_TOP_RIGHT
            isInHandle(x, y, cropRect.left, cropRect.bottom) -> HANDLE_BOTTOM_LEFT
            isInHandle(x, y, cropRect.right, cropRect.bottom) -> HANDLE_BOTTOM_RIGHT
            else -> HANDLE_NONE
        }
    }

    private fun isInHandle(x: Float, y: Float, handleX: Float, handleY: Float): Boolean {
        return abs(x - handleX) <= handleRadius * 2 && abs(y - handleY) <= handleRadius * 2
    }

    private fun updateCropRect(x: Float, y: Float) {
        val newX = x.coerceIn(0f, width.toFloat())
        val newY = y.coerceIn(0f, height.toFloat())

        when (selectedHandle) {
            HANDLE_TOP_LEFT -> {
                cropRect.left = min(newX, cropRect.right - handleRadius * 2)
                cropRect.top = min(newY, cropRect.bottom - handleRadius * 2)
            }
            HANDLE_TOP_RIGHT -> {
                cropRect.right = max(newX, cropRect.left + handleRadius * 2)
                cropRect.top = min(newY, cropRect.bottom - handleRadius * 2)
            }
            HANDLE_BOTTOM_LEFT -> {
                cropRect.left = min(newX, cropRect.right - handleRadius * 2)
                cropRect.bottom = max(newY, cropRect.top + handleRadius * 2)
            }
            HANDLE_BOTTOM_RIGHT -> {
                cropRect.right = max(newX, cropRect.left + handleRadius * 2)
                cropRect.bottom = max(newY, cropRect.top + handleRadius * 2)
            }
        }
    }

    fun getCropRect(): RectF = RectF(cropRect)
}