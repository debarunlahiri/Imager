package com.summitcodeworks.imager.activities

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.summitcodeworks.imager.R
import com.summitcodeworks.imager.databinding.ActivityImagePreviewBinding
import java.io.File

class ImagePreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImagePreviewBinding
    private lateinit var mContext: Context

    private lateinit var imageFile: File

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

        val imageFilePath = intent.getStringExtra("imageFilePath")
        if (imageFilePath != null) {
            imageFile = File(imageFilePath)
            // Use the file as needed
            Glide.with(mContext).load(imageFile).into(binding.myZoomageView)

        }



        binding.cvImagePreviewBack.setOnClickListener {
            onBackPressed()
        }

        binding.ivImagePreviewBack.setOnClickListener {
            onBackPressed()
        }

//        binding.bEditImage.setOnClickListener {
//            val intent = Intent(mContext, ImageEditActivity::class.java)
//            if (byteArray != null) {
//                intent.putExtra("imageBitmap", byteArray)
//            } else if (imageUriString != null) {
//                intent.putExtra("imageUri", imageUriString)
//            }
//            startActivity(intent)
//        }

        binding.bShareImage.setOnClickListener {
            if (::imageFile.isInitialized) {
                shareImage(imageFile)
            } else {
                Toast.makeText(mContext, "Image not found", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun shareImage(imageFile: File?) {
        val uri = imageFile?.let {
            androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                it
            )
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share image via"))
    }
}
