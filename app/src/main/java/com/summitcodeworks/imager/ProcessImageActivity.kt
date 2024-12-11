package com.summitcodeworks.imager

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.summitcodeworks.imager.databinding.ActivityProcessImageBinding
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream

class ProcessImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProcessImageBinding

    private lateinit var mContext: Context

    private lateinit var bitmap: Bitmap

    private var redStripData: RedStripData? = null
    private lateinit var redStripDataList: List<RedStripData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProcessImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mContext = this

        setupToolbar()

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed")
            Toast.makeText(this, "OpenCV initialization failed", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("OpenCV", "OpenCV initialized successfully")
            Toast.makeText(this, "OpenCV initialized successfully", Toast.LENGTH_SHORT).show()

            val imageUriString = intent.getStringExtra("imageUri")
            if (imageUriString != null) {
                val imageUri = Uri.parse(imageUriString)
                // Use the imageUri here
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                } else {
                    bitmap = CommonUtils.convertUriToBitmap(this, imageUri)!!
                }

                Log.d("ProcessImage", "Bitmap dimensions: ${bitmap.width}x${bitmap.height}")

                redStripDataList = processAndCountRedStrips(bitmap)

                binding.ivProcessInput.setImageBitmap(bitmap)
                binding.tvProcessCount.text = "Number of red strips: ${redStripDataList.count()}"


                val redStripAdapter = RedStripAdapter(redStripDataList, this, object : RedStripAdapter.OnRedStripAdapterListener {
                    override fun onRedStripAdapterClick(redStripData: RedStripData) {
                        val imagePreviewIntent = Intent(mContext, ImagePreviewActivity::class.java)
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        redStripData.bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                        val byteArray = byteArrayOutputStream.toByteArray()
                        imagePreviewIntent.putExtra("imageBitmap", byteArray)
                        startActivity(imagePreviewIntent)
                    }
                })
                binding.rvSegmentedImage.adapter = redStripAdapter
                binding.rvSegmentedImage.layoutManager = GridLayoutManager(this, 3)
            }
        }




    }

    fun processAndCountRedStrips(inputBitmap: Bitmap): List<RedStripData> {
        val resultList = mutableListOf<RedStripData>()

        if (inputBitmap.width <= 0 || inputBitmap.height <= 0) {
            Log.e("ProcessImage", "Invalid Bitmap input: Width or Height is 0")
            Toast.makeText(this, "Invalid Bitmap input", Toast.LENGTH_SHORT).show()
            return resultList // Return empty list if invalid input
        }

        Log.d("ProcessImage", "Input image dimensions: ${inputBitmap.width}x${inputBitmap.height}")

        // Load the image into OpenCV Mat
        val src = Mat()
        Utils.bitmapToMat(inputBitmap, src)

        // Convert to HSV color space
        val hsv = Mat()
        Imgproc.cvtColor(src, hsv, Imgproc.COLOR_RGB2HSV)

        // Adjusted HSV range for red color
        val lowerRed1 = Scalar(0.0, 70.0, 50.0)
        val upperRed1 = Scalar(10.0, 255.0, 255.0)
        val lowerRed2 = Scalar(160.0, 70.0, 50.0)
        val upperRed2 = Scalar(180.0, 255.0, 255.0)

        // Create masks for red color
        val mask1 = Mat()
        val mask2 = Mat()
        Core.inRange(hsv, lowerRed1, upperRed1, mask1)
        Core.inRange(hsv, lowerRed2, upperRed2, mask2)

        // Combine both masks
        val redMask = Mat()
        Core.add(mask1, mask2, redMask)

        // Apply Gaussian blur
        Imgproc.GaussianBlur(redMask, redMask, Size(3.0, 3.0), 0.0)

        // Morphological operations
        val kernel = Mat.ones(2, 2, CvType.CV_8U)
        Imgproc.erode(redMask, redMask, kernel)
        Imgproc.dilate(redMask, redMask, kernel)

        // Find contours
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(redMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        Log.d("ProcessImage", "Found ${contours.size} contours before filtering")

        // Reduced minimum contour area
        val minContourArea = 100.0
        val filteredContours = contours.filter {
            val area = Imgproc.contourArea(it)
            Log.d("ProcessImage", "Contour area: $area")
            area > minContourArea
        }

        Log.d("ProcessImage", "Found ${filteredContours.size} contours after filtering")

        if (filteredContours.isEmpty()) {
            Log.e("ProcessImage", "No red strips found")
            Toast.makeText(this, "No red strips found", Toast.LENGTH_SHORT).show()
            return resultList // Return empty list if no red strips are found
        }

        // Process each strip and store in the result list
        filteredContours.forEach { contour ->
            // Get the bounding rectangle for each contour
            val rect = Imgproc.boundingRect(contour)

            // Crop the red strip
            val strip = Mat(src, rect)

            // Resize to standard height
            val resizedStrip = Mat()
            Imgproc.resize(strip, resizedStrip, Size(src.width().toDouble(), 100.0))

            // Convert the result strip to Bitmap
            val outputBitmap = Bitmap.createBitmap(resizedStrip.cols(), resizedStrip.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(resizedStrip, outputBitmap)

            // Add the red strip data to the list
            resultList.add(RedStripData(filteredContours.size, outputBitmap))
        }

        Log.d("ProcessImage", "Processing completed. Found ${resultList.size} strips")

        return resultList
    }


    private fun setupToolbar() {
        binding.tbProcessImage.visibility = View.VISIBLE
        binding.tbProcessImage.title = "Final Image"

        setSupportActionBar(binding.tbProcessImage)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.tbProcessImage.setNavigationOnClickListener {
            finish()
        }

        val navigationIcon = ContextCompat.getDrawable(this, R.drawable.baseline_arrow_back_24)
        binding.tbProcessImage.setNavigationIcon(navigationIcon)

        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_NO -> {
                binding.tbProcessImage.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
                tintNavigationIcon(navigationIcon, R.color.white)
            }
            Configuration.UI_MODE_NIGHT_YES -> {
                binding.tbProcessImage.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
                tintNavigationIcon(navigationIcon, R.color.white)
            }
        }
    }

    private fun tintNavigationIcon(icon: Drawable?, colorResId: Int) {
        icon?.let {
            val wrappedIcon = DrawableCompat.wrap(it)
            DrawableCompat.setTint(wrappedIcon, ContextCompat.getColor(this, colorResId))
            binding.tbProcessImage.navigationIcon = wrappedIcon
        }
    }


}