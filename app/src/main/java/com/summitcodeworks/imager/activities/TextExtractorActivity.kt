package com.summitcodeworks.imager.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.summitcodeworks.imager.databinding.ActivityTextExtractorBinding
import java.io.InputStream

class TextExtractorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTextExtractorBinding
    private lateinit var mContext: Context
    private var originalBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextExtractorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mContext = this

        // Image picker
        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                originalBitmap = loadImageFromUri(it)
                originalBitmap?.let { bitmap ->
                    binding.imageView.setImageBitmap(bitmap)

                    originalBitmap?.let {
                        extractTextFromImage(it)
                    } ?: run {
                        Toast.makeText(mContext, "No image loaded", Toast.LENGTH_SHORT).show()
                    }
                } ?: run {
                    Toast.makeText(mContext, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }

        pickImage.launch("image/*")

        binding.bCopyToClipboard.setOnClickListener {
            val text = binding.etExtractedText.text.toString()
            if (text.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Extracted Text", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(mContext, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(mContext, "No text to copy", Toast.LENGTH_SHORT).show()
            }
        }

        binding.loadImageButton.setOnClickListener {
            pickImage.launch("image/*")
        }
    }

    // Load image from URI
    private fun loadImageFromUri(uri: Uri): Bitmap? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            null
        }
    }

    // Extract text using ML Kit's Text Recognition
    private fun extractTextFromImage(bitmap: Bitmap) {
        // Create an InputImage from the Bitmap
        val image = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)

        // Initialize TextRecognizer
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        // Process the image
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // Extracted text is in visionText
                val extractedText = visionText.text
                binding.etExtractedText.setText(extractedText)

                if (extractedText.isEmpty()) {
                    Toast.makeText(mContext, "No text found in the image", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(mContext, "Text extraction failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
