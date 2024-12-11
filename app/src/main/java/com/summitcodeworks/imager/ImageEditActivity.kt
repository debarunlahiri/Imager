package com.summitcodeworks.imager

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.summitcodeworks.imager.databinding.ActivityImageEditBinding

class ImageEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageEditBinding
    private var bitmap: Bitmap? = null
    private var canvasBitmap: Bitmap? = null
    private var scaleFactor = 1f
    private var rotationAngle = 0f
    private var translationX = 0f
    private var translationY = 0f
    private var canvasColor: Int = Color.LTGRAY // Default canvas color
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var isAddingCanvas = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageEditBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        // Load the image passed from the previous activity
        val byteArray = intent.getByteArrayExtra("imageBitmap")
        bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)
        Glide.with(this).load(bitmap).into(
            binding.imageView
        )

        // Initialize ScaleGestureDetector
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())

        // Rotate image by degrees
        binding!!.btnRotate.setOnClickListener { v ->
            rotationAngle += 90f
            applyTransformations()
        }

        // Add background canvas
        binding!!.btnAddBackground.setOnClickListener {
            val currentBitmap = bitmap
            if (currentBitmap != null) {
                // Check if a canvas is already added
                if (isAddingCanvas) {
                    // Update canvas with the new color
                    canvasBitmap = updateCanvasColor(currentBitmap, 800, 800, canvasColor)
                    binding.imageView.setImageBitmap(canvasBitmap)
                    bitmap = canvasBitmap // Update the bitmap reference
                    Toast.makeText(this, "Canvas color updated", Toast.LENGTH_SHORT).show()
                } else {
                    // Add a new canvas
                    canvasBitmap = addBackgroundCanvas(
                        currentBitmap,
                        800,
                        800,
                        canvasColor
                    )
                    binding.imageView.setImageBitmap(canvasBitmap)
                    bitmap = canvasBitmap // Update the bitmap reference
                    isAddingCanvas = true
                    Toast.makeText(this, "Canvas added", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Bitmap is null", Toast.LENGTH_SHORT).show()
            }
        }

        // Free rotation using SeekBar
        binding.rotationSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                rotationAngle = progress.toFloat()
                applyTransformations()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Handle touch events for scaling and dragging
        binding!!.imageView.setOnTouchListener { v, event ->
            scaleGestureDetector!!.onTouchEvent(event)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    translationX = event.x
                    translationY = event.y
                }

                MotionEvent.ACTION_MOVE -> {
                    translationX += event.x - translationX
                    translationY += event.y - translationY
                    applyTransformations()
                }
            }
            true
        }

        setupColorListeners()
    }

    private fun setupColorListeners() {
        binding.btnColorWhite.setOnClickListener {
            canvasColor = Color.WHITE
            showColorToast("White")
            refreshCanvasColor()
        }
        binding.btnColorBlack.setOnClickListener {
            canvasColor = Color.BLACK
            showColorToast("Black")
            refreshCanvasColor()
        }
        binding.btnColorRed.setOnClickListener {
            canvasColor = Color.RED
            showColorToast("Red")
            refreshCanvasColor()
        }
        binding.btnColorGreen.setOnClickListener {
            canvasColor = Color.GREEN
            showColorToast("Green")
            refreshCanvasColor()
        }
        binding.btnColorBlue.setOnClickListener {
            canvasColor = Color.BLUE
            showColorToast("Blue")
            refreshCanvasColor()
        }
        binding.btnColorGray.setOnClickListener {
            canvasColor = Color.DKGRAY
            showColorToast("Gray")
            refreshCanvasColor()
        }
    }

    private fun refreshCanvasColor() {
        if (isAddingCanvas) {
            val currentBitmap = bitmap
            if (currentBitmap != null) {
                // Update the canvas color based on the current selected color
                canvasBitmap = updateCanvasColor(currentBitmap, currentBitmap.width, currentBitmap.height, canvasColor)
                // Apply transformations to the new canvas bitmap
                applyTransformations()
            }
        }
    }

    private fun updateCanvasColor(image: Bitmap, width: Int, height: Int, color: Int): Bitmap {
        val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        canvas.drawColor(color) // Apply the new background color
        canvas.drawBitmap(
            image,
            (width - image.width) / 2f,
            (height - image.height) / 2f,
            null
        ) // Center the image on the new canvas
        return newBitmap
    }

    private fun applyTransformations() {
        if (bitmap != null) {
            val matrix = Matrix()
            matrix.postScale(scaleFactor, scaleFactor)
            matrix.postRotate(rotationAngle)
            matrix.postTranslate(translationX, translationY)

            // Apply transformations to the new canvas bitmap
            val transformedBitmap = Bitmap.createBitmap(
                bitmap!!, 0, 0, bitmap!!.width, bitmap!!.height, matrix, true
            )
            binding!!.imageView.setImageBitmap(transformedBitmap)
        }
    }

    private fun showColorToast(colorName: String) {
        Toast.makeText(this, "Canvas color set to $colorName", Toast.LENGTH_SHORT).show()
    }

    // Add a background canvas with specified dimensions and color
    private fun addBackgroundCanvas(image: Bitmap, width: Int, height: Int, color: Int): Bitmap {
        val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        canvas.drawColor(color) // Draw the background color
        canvas.drawBitmap(
            image,
            (width - image.width) / 2f,
            (height - image.height) / 2f,
            null
        ) // Center the image
        return newBitmap
    }

    // Scale gesture listener for zoom
    private inner class ScaleListener : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            applyTransformations()
            return true
        }
    }
}