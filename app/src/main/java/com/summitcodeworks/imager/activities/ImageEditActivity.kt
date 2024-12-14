package com.summitcodeworks.imager.activities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.summitcodeworks.imager.utils.CropView
import com.summitcodeworks.imager.databinding.ActivityImageEditBinding
import java.io.File
import java.io.FileOutputStream

class ImageEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageEditBinding
    private var originalBitmap: Bitmap? = null  // Store the original image
    private var bitmap: Bitmap? = null          // Current working bitmap
    private var canvasBitmap: Bitmap? = null
    private var scaleFactor = 1f
    private var rotationAngle = 0f
    private var translationX = 0f
    private var translationY = 0f
    private var canvasColor: Int = Color.LTGRAY
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var isAddingCanvas = false
    private var canvasWidth = 800
    private var canvasHeight = 800
    private var isCropping = false
    private var cropView: CropView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load the image passed from the previous activity (handle both imageBitmap and imageUri)
        val byteArray = intent.getByteArrayExtra("imageBitmap")
        val imageUriString = intent.getStringExtra("imageUri")

        if (byteArray != null) {
            // Handle byte array (imageBitmap)
            originalBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } else if (imageUriString != null) {
            // Handle URI (imageUri)
            val imageUri = Uri.parse(imageUriString)
            originalBitmap = contentResolver.openInputStream(imageUri)?.use { BitmapFactory.decodeStream(it) }
        }

        bitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)

        Glide.with(this).load(bitmap).into(binding.imageViewOriginal)

        binding.llColorPalette.visibility = View.GONE

        // Initialize ScaleGestureDetector
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())

        // Rotate image by degrees
        binding.btnRotate.setOnClickListener {
            rotationAngle += 90f
            applyTransformations()
        }

        // Add background canvas
        binding.btnAddBackground.setOnClickListener {
            val currentBitmap = originalBitmap
            if (currentBitmap != null) {
                canvasWidth = maxOf(800, currentBitmap.width)
                canvasHeight = maxOf(800, currentBitmap.height)

                if (!isAddingCanvas) {
                    // First time adding canvas
                    canvasBitmap = addBackgroundCanvas(currentBitmap, canvasWidth, canvasHeight, canvasColor)
                    bitmap = canvasBitmap
                    isAddingCanvas = true
                    Toast.makeText(this, "Canvas added", Toast.LENGTH_SHORT).show()
                }
                applyTransformations()
                binding.llColorPalette.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Bitmap is null", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup SeekBar
        binding.rotationSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                rotationAngle = progress.toFloat()
                applyTransformations()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Handle touch events
        binding.imageViewOriginal.setOnTouchListener { _, event ->
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

        binding.btnCrop.setOnClickListener {
            if (!isCropping) {
                startCropping()
            } else {
                applyCrop()
            }
        }

        binding.ibImageEditSave.setOnClickListener {
            saveCanvasWithImage()
        }

        setupColorListeners()
    }

    private fun startCropping() {
        isCropping = true
        binding.btnCrop.text = "Apply Crop"

        // Create and add CropView
        cropView = CropView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        binding.flImagePreview.addView(cropView)

        // Disable other controls while cropping
        binding.rotationSeekBar.isEnabled = false
        binding.btnRotate.isEnabled = false
        binding.btnAddBackground.isEnabled = false
        binding.imageViewOriginal.isEnabled = false
    }

    private fun applyCrop() {
        cropView?.let { cropView ->
            val cropRect = cropView.getCropRect()

            // Convert the crop rect to bitmap coordinates
            val imageView = binding.imageViewOriginal
            var bitmap = originalBitmap ?: return

            val viewToImageMatrix = Matrix()
            val imageMatrix = imageView.imageMatrix
            imageMatrix.invert(viewToImageMatrix)

            val bitmapRect = RectF()
            bitmapRect.set(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())

            val cropRectInBitmap = RectF()
            viewToImageMatrix.mapRect(cropRectInBitmap, cropRect)

            // Perform the crop
            try {
                val croppedBitmap = Bitmap.createBitmap(
                    bitmap,
                    cropRectInBitmap.left.toInt(),
                    cropRectInBitmap.top.toInt(),
                    cropRectInBitmap.width().toInt(),
                    cropRectInBitmap.height().toInt()
                )

                // Update bitmaps
                originalBitmap = croppedBitmap
                bitmap = croppedBitmap.copy(Bitmap.Config.ARGB_8888, true)

                // Update image view
                binding.imageViewOriginal.setImageBitmap(croppedBitmap)

                // If canvas was added, update it
                if (isAddingCanvas) {
                    updateCanvasWithCurrentColor()
                }
            } catch (e: IllegalArgumentException) {
                Toast.makeText(this, "Invalid crop area", Toast.LENGTH_SHORT).show()
            }
        }

        // Clean up
        binding.flImagePreview.removeView(cropView)
        cropView = null
        isCropping = false
        binding.btnCrop.text = "Crop"

        // Re-enable controls
        binding.rotationSeekBar.isEnabled = true
        binding.btnRotate.isEnabled = true
        binding.btnAddBackground.isEnabled = true
        binding.imageViewOriginal.isEnabled = true
    }

    private fun setupColorListeners() {
        val colorMap = mapOf(
            binding.btnColorWhite to Pair(Color.WHITE, "White"),
            binding.btnColorBlack to Pair(Color.BLACK, "Black"),
            binding.btnColorRed to Pair(Color.RED, "Red"),
            binding.btnColorGreen to Pair(Color.GREEN, "Green"),
            binding.btnColorBlue to Pair(Color.BLUE, "Blue"),
            binding.btnColorGray to Pair(Color.DKGRAY, "Gray")
        )

        colorMap.forEach { (button, colorInfo) ->
            button.setOnClickListener {
                canvasColor = colorInfo.first
                showColorToast(colorInfo.second)
                if (isAddingCanvas) {
                    updateCanvasWithCurrentColor()
                }
            }
        }
    }

    private fun updateCanvasWithCurrentColor() {
        originalBitmap?.let { original ->
            canvasBitmap = addBackgroundCanvas(original, canvasWidth, canvasHeight, canvasColor)
            bitmap = canvasBitmap
            applyTransformations()
        }
    }

    private fun applyTransformations() {
        val currentBitmap = if (isAddingCanvas) canvasBitmap else bitmap
        currentBitmap?.let { bmp ->
            val matrix = Matrix()
            matrix.postScale(scaleFactor, scaleFactor)
            matrix.postRotate(rotationAngle)
            matrix.postTranslate(translationX, translationY)

            val transformedBitmap = Bitmap.createBitmap(
                bmp, 0, 0, bmp.width, bmp.height, matrix, true
            )

            // Set the transformed bitmap to the ImageView
            if (isAddingCanvas) {
                binding.imageViewCanvas.setImageBitmap(transformedBitmap)
            } else {
                binding.imageViewOriginal.setImageBitmap(transformedBitmap)
            }
        }
    }

    private fun addBackgroundCanvas(image: Bitmap, width: Int, height: Int, color: Int): Bitmap {
        val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        canvas.drawColor(color)
        canvas.drawBitmap(
            image,
            (width - image.width) / 2f,
            (height - image.height) / 2f,
            null
        )
        return newBitmap
    }

    private fun showColorToast(colorName: String) {
        Toast.makeText(this, "Canvas color set to $colorName", Toast.LENGTH_SHORT).show()
    }

    private inner class ScaleListener : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            applyTransformations()
            return true
        }
    }

    private fun saveCanvasWithImage() {
        val currentBitmap = if (isAddingCanvas) canvasBitmap else originalBitmap

        if (currentBitmap != null) {
            try {
                // Create a merged bitmap with the canvas and image
                val mergedBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(mergedBitmap)

                // Draw the background canvas
                canvas.drawColor(canvasColor)

                // Draw the image centered on the canvas
                canvas.drawBitmap(
                    currentBitmap,
                    (canvasWidth - currentBitmap.width) / 2f,
                    (canvasHeight - currentBitmap.height) / 2f,
                    null
                )

                // Save the merged bitmap to a file
                val fileName = "merged_image_${System.currentTimeMillis()}.png"
                val file = File(applicationContext.filesDir, fileName)

                FileOutputStream(file).use { fos ->
                    mergedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }

                Toast.makeText(this, "Image saved: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show()
        }
    }

}
