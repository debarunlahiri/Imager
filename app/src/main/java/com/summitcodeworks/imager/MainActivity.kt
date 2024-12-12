package com.summitcodeworks.imager

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.summitcodeworks.imager.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val imageUris = mutableListOf<Uri>()

    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris?.let {
            imageUris.clear()
            imageUris.addAll(uris)
            if (imageUris.size > 0) {
                val intent = Intent(this, ImageProcessActivity::class.java)
                intent.putExtra("imageUri", imageUris[0].toString())
                startActivity(intent)
                return@registerForActivityResult
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        checkPermissions()


        binding.bCapture.setOnClickListener {
            val cameraIntent = Intent(this, CameraActivity::class.java)
            startActivity(cameraIntent)
        }

        binding.bChooseFIle.setOnClickListener {
            Toast.makeText(this, "Select only one image", Toast.LENGTH_SHORT).show()
            pickImages.launch("image/*")

        }

        binding.bGallery.setOnClickListener {
            val galleryIntent = Intent(this, GalleryActivity::class.java)
            galleryIntent.putExtra("isFromGallery", true)
            startActivity(galleryIntent)
        }

    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 (API level 33) and above, use READ_MEDIA_IMAGES
            val permissions = arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                if (results[android.Manifest.permission.READ_MEDIA_IMAGES] == true && results[android.Manifest.permission.WRITE_EXTERNAL_STORAGE] == true) {
                    // Permissions granted, proceed with loading images
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            launcher.launch(permissions)
        } else {
            // For Android versions below Android 13 (API 33), use READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE
            val permissions = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                if (results.all { it.value }) {
                    // Permissions granted, proceed with loading images
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            launcher.launch(permissions)
        }
    }

}