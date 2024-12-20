package com.summitcodeworks.imager.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.listeners.OnImageChangeListener
import com.summitcodeworks.imager.R
import com.summitcodeworks.imager.adapters.GalleryAdapter
import com.summitcodeworks.imager.adapters.ToolsAdapter
import com.summitcodeworks.imager.databinding.ActivityMainBinding
import com.summitcodeworks.imager.models.ToolsData
import com.summitcodeworks.imager.utils.CommonUtils
import org.opencv.android.OpenCVLoader
import java.io.File

class MainActivity : AppCompatActivity(), ToolsAdapter.OnToolsAdapterListener {

    private lateinit var binding: ActivityMainBinding

    private lateinit var mContext: Context

    private val imageUris = mutableListOf<Uri>()
    private lateinit var galleryAdapter: GalleryAdapter
    private lateinit var imageList: MutableList<File>

    private lateinit var toolsAdapter: ToolsAdapter
    private var toolsDataList: MutableList<ToolsData> = ArrayList<ToolsData>()

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

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.all { it.value }) {
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
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

        mContext = this
        CommonUtils.mContext = mContext

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed")
        } else {
            Log.d("OpenCV", "OpenCV initialized successfully")
        }

        launcher.launch(permissions)

        initToolsData()
        initTools()


//        binding.bCapture.setOnClickListener {
//            val cameraIntent = Intent(this, CameraActivity::class.java)
//            startActivity(cameraIntent)
//        }
//
//        binding.bChooseFIle.setOnClickListener {
//            Toast.makeText(this, "Select only one image", Toast.LENGTH_SHORT).show()
//            pickImages.launch("image/*")
//
//        }

        binding.cvMainGallery.setOnClickListener {
            val galleryIntent = Intent(this, GalleryActivity::class.java)
            galleryIntent.putExtra("isFromGallery", true)
            startActivity(galleryIntent)
        }

    }

    private fun initToolsData() {
        toolsDataList.add(ToolsData("grayscale", "Grayscale", R.drawable.ic_greyscale))
        toolsDataList.add(ToolsData("deepfake", "Deepfake", R.drawable.ic_deepfake))
        toolsDataList.add(ToolsData("background_remove", "Background Remover", R.drawable.ic_bg_remover))
        toolsDataList.add(ToolsData("text_extractor", "Text Extractor", R.drawable.ic_text_to_image))
        toolsDataList.add(ToolsData("meme_maker", "Meme Maker", R.drawable.ic_smile))
        toolsDataList.add(ToolsData("image_generator", "Image Generator", R.drawable.ic_text_to_image))
        toolsDataList.add(ToolsData("scan_document", "Scan Document", R.drawable.ic_scanner))
        toolsDataList.add(ToolsData("pdf_to_image", "PDF to Image", R.drawable.ic_pdf))
    }

    private fun initTools() {
        toolsAdapter = ToolsAdapter(mContext, toolsDataList, this)
        binding.rvMainTools.adapter = toolsAdapter
        binding.rvMainTools.layoutManager = GridLayoutManager(this, 3)
    }

    private fun initGallery() {
        if (::imageList.isInitialized) {
            imageList.clear()
        }
        imageList = loadImagesFromPrivateFolder()
        if (imageList.size == 0) {
            binding.llMainNoMedia.visibility = View.VISIBLE
            binding.rvMainGallery.visibility = View.GONE
        } else {
            binding.llMainNoMedia.visibility = View.GONE
            binding.rvMainGallery.visibility = View.VISIBLE
        }
        galleryAdapter = GalleryAdapter(imageList, mContext, supportFragmentManager)
        binding.rvMainGallery.adapter = galleryAdapter
        binding.rvMainGallery.layoutManager = GridLayoutManager(this, 3)
        galleryAdapter.notifyDataSetChanged()
    }

    private fun loadImagesFromPrivateFolder(): MutableList<File> {
        val imageFiles = mutableListOf<File>()
        val imageFolder = getExternalFilesDir(null) // Replace with getFilesDir() for internal storage

        imageFolder?.listFiles()?.sortedByDescending { it.lastModified() }?.forEach { file ->
            if (file.isFile && isImageFile(file.name)) {
                imageFiles.add(file)
            }
            // Stop if 6 files have been added
            if (imageFiles.size >= 6) {
                return@forEach
            }
        }

        // Limit the list to 6 files
        return imageFiles.take(6).toMutableList()
    }

    private fun isImageFile(fileName: String): Boolean {
        val extensions = listOf("jpg", "jpeg", "png", "gif", "bmp")
        return extensions.any { fileName.endsWith(it, ignoreCase = true) }
    }

    override fun onToolsAdapterClick(toolsData: ToolsData) {
        when (toolsData.tool_id) {
            "grayscale" -> {
                val intent = Intent(this, ImageEnhanceActivity::class.java)
                startActivity(intent)
            }
            "deepfake" -> {
                val intent = Intent(this, RemovePeopleActivity::class.java)
                startActivity(intent)
            }
            "background_remove" -> {
                val intent = Intent(this, BackgroundRemoverActivity::class.java)
                startActivity(intent)
            }
            "text_extractor" -> {
                val intent = Intent(this, TextExtractorActivity::class.java)
                startActivity(intent)
            }
            "meme_maker" -> {
                val intent = Intent(this, MemeMakerActivity::class.java)
                startActivity(intent)
            }
            "image_generator" -> {
                val intent = Intent(this, ImageGeneratorActivity::class.java)
                startActivity(intent)
            }
            "scan_document" -> {
                val intent = Intent(this, DocumentScanActivity::class.java)
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        initGallery()

    }


}