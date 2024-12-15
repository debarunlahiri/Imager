package com.summitcodeworks.imager.activities

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.summitcodeworks.imager.R
import com.summitcodeworks.imager.databinding.ActivityImageGeneratorBinding
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ImageGeneratorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageGeneratorBinding

    private lateinit var mContext: Context

    private lateinit var tflite: Interpreter

    private val DEBUG = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageGeneratorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mContext = this

        val options = Interpreter.Options().apply {
            setNumThreads(1)  // Reduce complexity for debugging
            setUseNNAPI(false)  // Disable NNAPI to get more detailed error messages
        }


        // Initialize the TFLite interpreter
        tflite = Interpreter(loadModelFile(), options)

        logModelInfo()

        binding.btnGenerate.setOnClickListener {
            val prompt = binding.etPrompt.text.toString()
            if (prompt.isNotBlank()) {
                try {
                    val generatedImage = generateImageFromText(prompt)
                    binding.ivResult.setImageBitmap(generatedImage)
                } catch (e: Exception) {
                    Log.e("ImageGenerator", "Error during generation", e)
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Please enter a prompt", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logModelInfo() {
        val inputTensor = tflite.getInputTensor(0)
        val outputTensor = tflite.getOutputTensor(0)

        Log.d("ModelInfo", "Input Tensor Shape: ${inputTensor.shape().contentToString()}")
        Log.d("ModelInfo", "Input Tensor Type: ${inputTensor.dataType()}")
        Log.d("ModelInfo", "Output Tensor Shape: ${outputTensor.shape().contentToString()}")
        Log.d("ModelInfo", "Output Tensor Type: ${outputTensor.dataType()}")
    }

    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = assets.openFd("diffusion_model.tflite")
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }


    private fun generateImageFromText(prompt: String): Bitmap {
        // Get input shape from the model
        val inputTensor = tflite.getInputTensor(0)
        val requiredFloats = 236544 / 4 // Total bytes / 4 bytes per float

        // Create input buffer
        val inputBuffer = ByteBuffer.allocateDirect(236544).apply {
            order(ByteOrder.nativeOrder())
        }

        // Process text into embeddings
        val embeddings = generateEmbeddings(prompt, requiredFloats)

        // Fill buffer
        embeddings.forEach { inputBuffer.putFloat(it) }
        inputBuffer.rewind()

        // Setup output buffer
        val outputTensor = tflite.getOutputTensor(0)
        val outputShape = outputTensor.shape()
        val outputSize = outputShape[1] * outputShape[2] * outputShape[3] * 4
        val outputBuffer = ByteBuffer.allocateDirect(outputSize).apply {
            order(ByteOrder.nativeOrder())
        }

        // Run inference
        tflite.run(inputBuffer, outputBuffer)

        return processOutput(outputBuffer, outputShape[1], outputShape[2])
    }

    private fun generateEmbeddings(prompt: String, size: Int): FloatArray {
        // Start with a baseline value that won't cause division by zero
        val baseValue = 1.0f

        // Create embeddings array
        return FloatArray(size) { index ->
            if (index < prompt.length) {
                // Convert character to a safe float value
                // Scale to ensure we're always significantly above zero
                val charValue = prompt[index].code.toFloat()
                baseValue + (charValue / 255.0f)  // This ensures values are always > 1.0
            } else {
                // Padding values, using base value to avoid zeros
                baseValue
            }
        }
    }

    private fun processTextWithValidation(prompt: String, buffer: ByteBuffer) {
        buffer.rewind()

        // Ensure we have valid text input
        val safePrompt = prompt.takeIf { it.isNotBlank() } ?: "default"

        // Calculate embedding with careful bounds
        val floatsNeeded = buffer.capacity() / 4
        for (i in 0 until floatsNeeded) {
            val value = if (i < safePrompt.length) {
                // Normalize to range [0.1, 1.0] to avoid zeros
                0.1f + (safePrompt[i].code.toFloat() / 255.0f * 0.9f)
            } else {
                // Use small non-zero value for padding
                0.1f
            }
            buffer.putFloat(value)
        }

        buffer.rewind()
    }

    private fun logBufferContents(buffer: ByteBuffer, bufferName: String) {
        buffer.rewind()
        val values = mutableListOf<Float>()
        while (buffer.hasRemaining()) {
            values.add(buffer.float)
        }
        buffer.rewind()

        Log.d("BufferContent", "$bufferName Buffer Statistics:")
        Log.d("BufferContent", "Min: ${values.minOrNull()}")
        Log.d("BufferContent", "Max: ${values.maxOrNull()}")
        Log.d("BufferContent", "Average: ${values.average()}")
        Log.d("BufferContent", "Zero count: ${values.count { it == 0f }}")
    }

    private fun processOutput(outputBuffer: ByteBuffer, width: Int, height: Int): Bitmap {
        outputBuffer.rewind()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)

        for (i in pixels.indices) {
            val r = (outputBuffer.float.coerceIn(0f, 1f) * 255).toInt()
            val g = (outputBuffer.float.coerceIn(0f, 1f) * 255).toInt()
            val b = (outputBuffer.float.coerceIn(0f, 1f) * 255).toInt()
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }


    override fun onDestroy() {
        super.onDestroy()
        tflite.close()
    }
}
