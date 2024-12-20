package com.summitcodeworks.imager.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import android.view.Gravity
import android.widget.LinearLayout
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator

class CustomRulerWithReset @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val rulerView: CustomRulerView
    private val resetButton: ImageButton

    init {
        orientation = VERTICAL

        // Create ruler view
        rulerView = CustomRulerView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        // Create reset button
        resetButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_revert)
            background = ContextCompat.getDrawable(context, android.R.drawable.btn_default)
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                topMargin = 16 // Add some space between ruler and button
            }
            setOnClickListener {
                rulerView.resetToCenter()
            }
        }

        // Add views to layout
        addView(rulerView)
        addView(resetButton)
    }
}

class CustomRulerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.BLACK
    }

    private val backgroundPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
    }

    private val bounds = Rect()
    private val totalTicks = 200
    private var currentValue = 2.1f
    private var scrollOffset = 0f
    private var lastTouchX = 0f
    private val tickSpacing = 30f
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private var lastTickPosition = 0

    fun resetToCenter() {
        // Animate the scroll offset back to center
        val animator = ValueAnimator.ofFloat(scrollOffset, 0f)
        animator.duration = 300 // Duration in milliseconds
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            scrollOffset = animation.animatedValue as Float
            currentValue = 2.1f - (scrollOffset / tickSpacing) / 10f
            invalidate()
        }
        animator.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.x - lastTouchX
                scrollOffset += deltaX

                scrollOffset = scrollOffset.coerceIn(
                    -(totalTicks * tickSpacing) / 2,
                    (totalTicks * tickSpacing) / 2
                )

                val currentTickPosition = (scrollOffset / tickSpacing).toInt()

                if (currentTickPosition != lastTickPosition) {
                    provideHapticFeedback()
                    lastTickPosition = currentTickPosition
                }

                currentValue = 2.1f - (scrollOffset / tickSpacing) / 10f

                lastTouchX = event.x
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun provideHapticFeedback() {
        val effect = VibrationEffect.createOneShot(1, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(effect)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2
        val centerX = width / 2

        canvas.drawLine(0f, centerY, width, centerY, paint)

        val visibleTicks = (width / tickSpacing).toInt() + 2
        val firstVisibleTick = (scrollOffset / tickSpacing).toInt() - visibleTicks / 2
        val lastVisibleTick = firstVisibleTick + visibleTicks

        for (i in firstVisibleTick..lastVisibleTick) {
            val x = centerX + (i * tickSpacing) - scrollOffset

            val tickHeight = when {
                i == 0 -> 40f
                i % 5 == 0 -> 25f
                else -> 15f
            }

            canvas.drawLine(
                x,
                centerY - tickHeight / 2,
                x,
                centerY + tickHeight / 2,
                paint
            )
        }

        val displayValue = String.format("%.1f°", currentValue)
        textPaint.getTextBounds(displayValue, 0, displayValue.length, bounds)
        canvas.drawText(
            displayValue,
            centerX,
            centerY - 30f,
            textPaint
        )

        paint.color = Color.RED
        canvas.drawLine(
            centerX,
            centerY - 20f,
            centerX,
            centerY + 20f,
            paint
        )
        paint.color = Color.BLACK
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 400
        val desiredHeight = 100

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> desiredWidth.coerceAtMost(widthSize)
            else -> desiredWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> desiredHeight.coerceAtMost(heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }
}