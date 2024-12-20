package com.summitcodeworks.imager.activities

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.stfalcon.imageviewer.StfalconImageViewer
import com.summitcodeworks.imager.adapters.GalleryAdapter
import com.summitcodeworks.imager.R
import com.summitcodeworks.imager.databinding.ActivityGalleryBinding
import com.summitcodeworks.imager.fragments.ImageActionsBottomDialogFragment
import java.io.File

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var mContext: Context
    private lateinit var galleryAdapter: GalleryAdapter
    private lateinit var imageList: MutableList<File>
    private var isFromGallery: Boolean = false

    // Register activity result launcher for picking images


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mContext = this

        isFromGallery = intent.getBooleanExtra("isFromGallery", false)


        imageList = loadImagesFromPrivateFolder()

        galleryAdapter = GalleryAdapter(imageList, mContext, supportFragmentManager)
        binding.rvGallery.adapter = galleryAdapter
        binding.rvGallery.layoutManager = GridLayoutManager(this, 3)

        setupToolbar()
        checkPermissions()
        setupPagination()
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
                binding.tbGallery.setTitleTextColor(ContextCompat.getColor(this, R.color.text_primary))
                tintNavigationIcon(navigationIcon, R.color.text_primary)
            }
            Configuration.UI_MODE_NIGHT_YES -> {
                binding.tbGallery.setTitleTextColor(ContextCompat.getColor(this, R.color.text_primary))
                tintNavigationIcon(navigationIcon, R.color.text_primary)
            }
        }
    }

    private fun loadImagesFromPrivateFolder(): MutableList<File> {
        val imageFiles = mutableListOf<File>()
        val imageFolder = getExternalFilesDir(null) // Replace with getFilesDir() for internal storage
        imageFolder?.listFiles()?.forEach { file ->
            if (file.isFile && isImageFile(file.name)) {
                imageFiles.add(file)
            }
        }
        return imageFiles.asReversed().toMutableList()
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



}
