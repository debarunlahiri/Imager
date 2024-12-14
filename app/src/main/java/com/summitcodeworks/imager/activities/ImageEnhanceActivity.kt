package com.summitcodeworks.imager.activities

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.summitcodeworks.imager.R
import com.summitcodeworks.imager.databinding.ActivityImageEnhanceBinding
import com.summitcodeworks.imager.utils.ImageProcessor
import org.opencv.android.Utils
import org.opencv.core.Mat
import java.io.File
import java.io.FileOutputStream

class ImageEnhanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageEnhanceBinding

    private lateinit var mContext: Context

    private var originalBitmap: Bitmap? = null
    private var enhancedBitmap: Bitmap? = null
    private lateinit var file: File

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
        binding = ActivityImageEnhanceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mContext = this

        binding.loadImageButton.setOnClickListener {
            pickImage()
        }

        binding.enhanceButton.setOnClickListener {
            if (originalBitmap != null) {
                enhanceImage(originalBitmap!!)
            } else {
                Toast.makeText(this, "Load an image first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cvEnhanceBack.setOnClickListener {
            finish()
        }

        binding.bSaveImage.setOnClickListener {
            if (enhancedBitmap != null) {
                val file = saveImageToPrivateFolder(enhancedBitmap!!)
                file?.let {
                    shareImage(file)
                }
            } else {
                Toast.makeText(this, "Enhance an image first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.bShareImage.setOnClickListener {
            if (::file.isInitialized) {
                if (file.exists()) {
                    shareImage(file)
                } else {
                    Toast.makeText(this, "Save the image first", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Save the image first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        launcher.launch(intent)
    }

    private fun enhanceImage(bitmap: Bitmap) {
        try {
            val src = Mat()
            val dst = Mat()
            Utils.bitmapToMat(bitmap, src)

            // Call the processing function
            ImageProcessor.enhanceImage(src, dst)

            enhancedBitmap = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(dst, enhancedBitmap)
            binding.imageView.setImageBitmap(enhancedBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(mContext, "Error while enhancing image", Toast.LENGTH_SHORT).show()
        }

    }

    private fun saveImageToPrivateFolder(bitmap: Bitmap): File? {
        try {
            val fileName = "enhanced_image_${System.currentTimeMillis()}.png"
            val directory = File(getExternalFilesDir(null), "")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            file = File(directory, fileName)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show()
        }
        return null
    }


    private fun shareImage(file: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share image via"))
    }


}