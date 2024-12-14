package com.summitcodeworks.imager.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.summitcodeworks.imager.databinding.ActivityRemovePeopleBinding
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil

class RemovePeopleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRemovePeopleBinding
    private var originalBitmap: Bitmap? = null
    private lateinit var interpreter: Interpreter

    val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val uri = result.data!!.data
            uri?.let {
                val inputStream = contentResolver.openInputStream(it)
                originalBitmap = BitmapFactory.decodeStream(inputStream)
                binding.imageView.setImageBitmap(originalBitmap)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRemovePeopleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize TensorFlow Lite interpreter
        interpreter = Interpreter(FileUtil.loadMappedFile(this, "deeplabv3.tflite"))

        binding.bSaveImage.setOnClickListener {
            originalBitmap?.let {
                processImage(it)
            } ?: run {
                Toast.makeText(this, "No image loaded", Toast.LENGTH_SHORT).show()
            }
        }

        binding.loadImageButton.setOnClickListener {
            pickImage()
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        launcher.launch(intent)
    }

    private fun processImage(bitmap: Bitmap) {
        try {
            val inputBitmap = Bitmap.createScaledBitmap(bitmap, 513, 513, true)
            val inputTensor = preprocessImage(inputBitmap)

            // Prepare output tensor
            val outputShape = interpreter.getOutputTensor(0).shape()  // Usually [1, 513, 513, num_classes]
            val outputBuffer: Array<Array<Array<FloatArray>>> = Array(outputShape[1]) {
                arrayOf(Array(outputShape[2]) { FloatArray(outputShape[3]) })
            }

            // Run model inference
            interpreter.run(inputTensor, outputBuffer)

            // Create a list of segmented images
            val segmentedImages = createSegmentedImages(outputBuffer, inputBitmap.width, inputBitmap.height)

            // Handle segmented images (for example, display the first image)
            segmentedImages.firstOrNull()?.let {
                binding.imageView.setImageBitmap(it)
            }

            // Optionally, save or process more segmented images from the list
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun preprocessImage(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val inputArray = Array(1) { Array(bitmap.height) { Array(bitmap.width) { FloatArray(3) } } }
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                inputArray[0][y][x][0] = Color.red(pixel) / 255.0f
                inputArray[0][y][x][1] = Color.green(pixel) / 255.0f
                inputArray[0][y][x][2] = Color.blue(pixel) / 255.0f
            }
        }
        return inputArray
    }

    private fun createSegmentedImages(
        outputBuffer: Array<Array<Array<FloatArray>>>,
        width: Int,
        height: Int
    ): List<Bitmap> {
        val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val peopleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val segmentedImages = mutableListOf<Bitmap>()

        // Create the mask and extract people
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixelClasses = outputBuffer[0][y][x]
                val maxClassIndex = pixelClasses.indices.maxByOrNull { pixelClasses[it] } ?: 0

                // Check if the max class is "person" (class index 15 for DeepLabv3)
                if (maxClassIndex == 15) {
                    val color = Color.argb(255, 255, 255, 255) // White for "person"
                    maskBitmap.setPixel(x, y, color)

                    // Extract person from original image
                    val originalPixel = originalBitmap?.getPixel(x, y) ?: Color.TRANSPARENT
                    peopleBitmap.setPixel(x, y, originalPixel)
                } else {
                    // Set transparent for non-person regions
                    maskBitmap.setPixel(x, y, Color.argb(0, 0, 0, 0))
                    peopleBitmap.setPixel(x, y, Color.TRANSPARENT)
                }
            }
        }

        // Add generated bitmaps to list
        segmentedImages.add(maskBitmap)
        segmentedImages.add(peopleBitmap)

        return segmentedImages
    }

    private fun loadImageFromUri(uri: Uri): Bitmap? {
        return try {
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            null
        }
    }
}
