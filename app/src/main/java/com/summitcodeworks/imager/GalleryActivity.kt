package com.summitcodeworks.imager

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.summitcodeworks.imager.databinding.ActivityGalleryBinding
import java.io.ByteArrayOutputStream
import java.io.File

class GalleryActivity : AppCompatActivity(), GalleryAdapter.OnGalleryListener {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var mContext: Context
    private lateinit var galleryAdapter: GalleryAdapter
    private lateinit var imageList: MutableList<Uri>
    private var isFromGallery: Boolean = false

    // Register activity result launcher for picking images


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mContext = this

        isFromGallery = intent.getBooleanExtra("isFromGallery", false)

        if (isFromGallery) {
            binding.bProcessImage.visibility = View.GONE
        } else {
            binding.bProcessImage.visibility = View.VISIBLE
        }

        imageList = loadImagesFromPrivateFolder()

        galleryAdapter = GalleryAdapter(imageList, mContext, this)
        binding.rvGallery.adapter = galleryAdapter
        binding.rvGallery.layoutManager = GridLayoutManager(this, 3)

        setupToolbar()
        checkPermissions()
        setupPagination()


        binding.bProcessImage.setOnClickListener {
            // Process the selected image
            // For example, you can pass the selected image URI to another activity
            // using an Intent

        }
    }

    private fun setupToolbar() {
        binding.tbGallery.visibility = View.VISIBLE
        if (isFromGallery) {
            binding.tbGallery.title = "Gallery"
        } else {
            binding.tbGallery.title = "Select Image"
        }

        setSupportActionBar(binding.tbGallery)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.tbGallery.setNavigationOnClickListener {
            finish()
        }

        val navigationIcon = ContextCompat.getDrawable(this, R.drawable.baseline_arrow_back_24)
        binding.tbGallery.setNavigationIcon(navigationIcon)

        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_NO -> {
                binding.tbGallery.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
                tintNavigationIcon(navigationIcon, R.color.white)
            }
            Configuration.UI_MODE_NIGHT_YES -> {
                binding.tbGallery.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
                tintNavigationIcon(navigationIcon, R.color.white)
            }
        }
    }

    private fun loadImagesFromPrivateFolder(): MutableList<Uri> {
        val imageUris = mutableListOf<Uri>()
        val imageFolder = getExternalFilesDir(null) // Replace with getFilesDir() for internal storage

        imageFolder?.listFiles()?.forEach { file ->
            if (file.isFile && isImageFile(file.name)) {
                imageUris.add(Uri.fromFile(file))
            }
        }
        return imageUris
    }

    private fun isImageFile(fileName: String): Boolean {
        val extensions = listOf("jpg", "jpeg", "png", "gif", "bmp")
        return extensions.any { fileName.endsWith(it, ignoreCase = true) }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissions = arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
            val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                if (results[android.Manifest.permission.READ_MEDIA_IMAGES] == true) {
                    // Permissions granted, proceed with loading images
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            launcher.launch(permissions)
        } else {
            val permissions = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
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

    // Load more images when the user scrolls to the bottom of the RecyclerView
    private fun setupPagination() {
        binding.rvGallery.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                // If we are at the bottom of the list, load the next page
                if (visibleItemCount + firstVisibleItemPosition >= totalItemCount) {
                    // No longer required with image picker
                }
            }
        })
    }

    private fun tintNavigationIcon(icon: Drawable?, colorResId: Int) {
        icon?.let {
            val wrappedIcon = DrawableCompat.wrap(it)
            DrawableCompat.setTint(wrappedIcon, ContextCompat.getColor(this, colorResId))
            binding.tbGallery.navigationIcon = wrappedIcon
        }
    }

    override fun onGalleryClick(imageUri: Uri) {
        val imagePreviewIntent = Intent(mContext, ImagePreviewActivity::class.java)
        imagePreviewIntent.putExtra("imageUri", imageUri.toString())
        startActivity(imagePreviewIntent)
    }

    override fun onGalleryDelete(imageUri: Uri) {
        try {
            // Convert Uri to File
            val file = File(imageUri.path ?: "")
            // Check if file exists and delete it
            if (file.exists()) {
                val isDeleted = file.delete()
                if (isDeleted) {
                    // If successfully deleted, remove it from the list
                    imageList.remove(imageUri)
                    galleryAdapter.notifyItemRemoved(imageList.indexOf(imageUri))
                    Toast.makeText(mContext, "Image deleted successfully.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(mContext, "Failed to delete the image.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(mContext, "File does not exist.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(mContext, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

}
