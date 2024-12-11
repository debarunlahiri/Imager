package com.summitcodeworks.imager

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var mContext: Context
    private val imageUris = mutableListOf<Uri>()
    private lateinit var galleryAdapter: GalleryAdapter

    // Register activity result launcher for picking images
    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris?.let {
            imageUris.clear()
            imageUris.addAll(uris)
            galleryAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mContext = this

        galleryAdapter = GalleryAdapter(imageUris, mContext) { uri ->
            Toast.makeText(this, "Selected: $uri", Toast.LENGTH_SHORT).show()
        }
        binding.rvGallery.adapter = galleryAdapter

        // Set the LayoutManager for RecyclerView
        binding.rvGallery.layoutManager = GridLayoutManager(this, 3)

        setupToolbar()
        checkPermissions()
        setupImagePicker()
        setupPagination()


        binding.bProcessImage.setOnClickListener {
            // Process the selected image
            // For example, you can pass the selected image URI to another activity
            // using an Intent
            val intent = Intent(this, ProcessImageActivity::class.java)
            intent.putExtra("imageUri", imageUris[0].toString())
            startActivity(intent)
        }
    }

    private fun setupToolbar() {
        binding.tbGallery.visibility = View.VISIBLE
        binding.tbGallery.title = "Gallery"

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

    private fun setupImagePicker() {
        // Set up the button or any view that triggers the image picker
        pickImages.launch("image/*")
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
}
