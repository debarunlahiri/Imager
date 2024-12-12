package com.summitcodeworks.imager

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.summitcodeworks.imager.databinding.ActivityImagePreviewBinding
import java.io.File

class ImagePreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImagePreviewBinding
    private lateinit var mContext: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mContext = this

        // Handle both byte array and URI for image preview
        val byteArray = intent.getByteArrayExtra("imageBitmap")
        val imageUriString = intent.getStringExtra("imageUri")

        if (byteArray != null) {
            // Display image from byte array
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            Glide.with(mContext).load(bitmap).into(binding.myZoomageView)
        } else if (imageUriString != null) {
            // Display image from URI
            val imageUri = Uri.parse(imageUriString)
            Glide.with(mContext).load(imageUri).into(binding.myZoomageView)
        }

        binding.cvImagePreviewBack.setOnClickListener {
            onBackPressed()
        }

        binding.ivImagePreviewBack.setOnClickListener {
            onBackPressed()
        }

        binding.bEditImage.setOnClickListener {
            val intent = Intent(mContext, ImageEditActivity::class.java)
            if (byteArray != null) {
                intent.putExtra("imageBitmap", byteArray)
            } else if (imageUriString != null) {
                intent.putExtra("imageUri", imageUriString)
            }
            startActivity(intent)
        }

        binding.bShareImage.setOnClickListener {
            if (byteArray != null) {
                // If image is passed as byte array, save it and share
                val imageFile = saveByteArrayToCache(byteArray)
                if (imageFile != null) {
                    val imageUri = FileProvider.getUriForFile(
                        this,
                        "${applicationContext.packageName}.fileprovider",
                        imageFile
                    )
                    shareImage(imageUri)
                } else {
                    Toast.makeText(this, "Failed to share image", Toast.LENGTH_SHORT).show()
                }
            } else if (imageUriString != null) {
                // If image is passed as URI, share the URI directly
                val imageUri = Uri.parse(imageUriString)
                shareImage(imageUri)
            }
        }
    }

    private fun saveByteArrayToCache(byteArray: ByteArray): File? {
        return try {
            val cacheDir = cacheDir
            val file = File(cacheDir, "shared_image_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { it.write(byteArray) }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun shareImage(imageUri: Uri) {
//        val intent = Intent(Intent.ACTION_SEND).apply {
//            type = "image/*"
//            putExtra(Intent.EXTRA_STREAM, imageUri)
//            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        }
//        startActivity(Intent.createChooser(intent, "Share Image"))

        Toast.makeText(this, "Functionality not supported right now", Toast.LENGTH_SHORT).show()
    }
}
