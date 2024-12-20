package com.summitcodeworks.imager.activities

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import com.summitcodeworks.imager.fragments.SelectImageBottomDialogFragment
import com.summitcodeworks.imager.utils.CommonUtils
import java.io.InputStream

class TextExtractorActivity : AppCompatActivity(), SelectImageBottomDialogFragment.OnSelectImageListener {

    private lateinit var binding: ActivityTextExtractorBinding
    private lateinit var mContext: Context
    private var originalBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextExtractorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mContext = this

//        // Image picker
//        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
//            uri?.let {
//                originalBitmap = loadImageFromUri(it)
//                originalBitmap?.let { bitmap ->
//                    binding.imageView.setImageBitmap(bitmap)
//
//                    originalBitmap?.let {
//                        extractTextFromImage(it)
//                    } ?: run {
//                        Toast.makeText(mContext, "No image loaded", Toast.LENGTH_SHORT).show()
//                    }
//                } ?: run {
//                    Toast.makeText(mContext, "Failed to load image", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//
//        pickImage.launch("image/*")

        showSelectImageDialog()

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
            showSelectImageDialog()
        }
    }

    private fun showSelectImageDialog() {
        val selectImageBottomDialogFragment = SelectImageBottomDialogFragment.newInstance("", "")
        selectImageBottomDialogFragment.show(supportFragmentManager, "select_image_bottom_dialog")
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


    override fun onCameraClick() {
        launchCamera()
    }

    override fun onGalleryClick() {
        pickImage()
    }

    override fun onDialogDismiss() {
        finish()
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        launcher.launch(intent)
    }

    val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val uri = result.data!!.data
            uri?.let {
                val inputStream = contentResolver.openInputStream(it)
                originalBitmap = BitmapFactory.decodeStream(inputStream)
                handleImage(originalBitmap!!)
            }
        }
    }

    private fun launchCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        cameraLauncher.launch(intent)
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.getStringExtra(CameraActivity.EXTRA_IMAGE_URI)

            if (imageUri != null) {
                val uri = Uri.parse(imageUri)
                originalBitmap = CommonUtils.convertUriToBitmap(this, uri)!!
                binding.imageView.setImageURI(uri)
                handleImage(originalBitmap!!)
            } else {
                Toast.makeText(this, "Failed to get image URI", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Image capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleImage(originalBitmap: Bitmap) {
        binding.imageView.setImageBitmap(originalBitmap)
    }
}
