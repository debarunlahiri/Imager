package com.summitcodeworks.imager.activities

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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.summitcodeworks.imager.utils.CommonUtils
import com.summitcodeworks.imager.R
import com.summitcodeworks.imager.adapters.RedStripAdapter
import com.summitcodeworks.imager.models.RedStripData
import com.summitcodeworks.imager.databinding.ActivityImageProcessBinding
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.RotatedRect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ImageProcessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageProcessBinding

    private lateinit var mContext: Context

    private lateinit var bitmap: Bitmap

    private var redStripData: RedStripData? = null
    private lateinit var redStripDataList: MutableList<RedStripData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageProcessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mContext = this

        setupToolbar()

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed")
            Toast.makeText(this, "Image failed to process. Please try again later.", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("OpenCV", "OpenCV initialized successfully")

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

                    override fun onRedStripAdapterDelete(redStripData: RedStripData) {
                        try {
                            redStripDataList.remove(redStripData) // Assuming you have a list named `redStripList`
                            Toast.makeText(mContext, "Image and deleted successfully.", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(mContext, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
                binding.rvSegmentedImage.adapter = redStripAdapter
                binding.rvSegmentedImage.layoutManager = GridLayoutManager(this, 3)
            }
        }
    }

    fun processAndCountRedStrips(inputBitmap: Bitmap): MutableList<RedStripData> {
        val resultList = mutableListOf<RedStripData>()

        if (inputBitmap.width <= 0 || inputBitmap.height <= 0) {
            Log.e("ProcessImage", "Invalid Bitmap input: Width or Height is 0")
            Toast.makeText(this, "Invalid Bitmap input", Toast.LENGTH_SHORT).show()
            return resultList
        }

        Log.d("ProcessImage", "Input image dimensions: ${inputBitmap.width}x${inputBitmap.height}")

        // Load the image into OpenCV Mat
        val src = Mat()
        Utils.bitmapToMat(inputBitmap, src)

        // Convert to HSV color space
        val hsv = Mat()
        Imgproc.cvtColor(src, hsv, Imgproc.COLOR_RGB2HSV)

        // Red color range detection (same as before)
        val lowerRed1 = Scalar(0.0, 70.0, 50.0)
        val upperRed1 = Scalar(10.0, 255.0, 255.0)
        val lowerRed2 = Scalar(160.0, 70.0, 50.0)
        val upperRed2 = Scalar(180.0, 255.0, 255.0)

        val mask1 = Mat()
        val mask2 = Mat()
        Core.inRange(hsv, lowerRed1, upperRed1, mask1)
        Core.inRange(hsv, lowerRed2, upperRed2, mask2)
        val redMask = Mat()
        Core.add(mask1, mask2, redMask)

        // Enhanced preprocessing
        Imgproc.GaussianBlur(redMask, redMask, Size(3.0, 3.0), 0.0)
        val kernel = Mat.ones(2, 2, CvType.CV_8U)
        Imgproc.erode(redMask, redMask, kernel)
        Imgproc.dilate(redMask, redMask, kernel)

        // Find contours
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(redMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        // Filter and process contours
        val minContourArea = 100.0
        val filteredContours = contours.filter { Imgproc.contourArea(it) > minContourArea }

        filteredContours.forEach { contour ->
            // Find the minimum area rectangle
            val points = MatOfPoint2f()
            contour.convertTo(points, CvType.CV_32F)
            val minAreaRect = Imgproc.minAreaRect(points)

            // Get the angle of rotation
            var angle = minAreaRect.angle

            // Adjust angle interpretation
            if (minAreaRect.size.width < minAreaRect.size.height) {
                angle += 90
            }

            // Create rotation matrix
            val rotationMatrix = Imgproc.getRotationMatrix2D(
                minAreaRect.center,
                angle,
                1.0
            )

            // Get the bounding rectangle for the original contour
            val rect = Imgproc.boundingRect(contour)

            // Create a mask for this specific strip
            val stripMask = Mat.zeros(src.size(), CvType.CV_8UC1)
            Imgproc.drawContours(
                stripMask,
                listOf(contour),
                0,
                Scalar(255.0),
                -1
            )

            // Extract the strip using the mask
            val strip = Mat()
            src.copyTo(strip, stripMask)

            // Rotate the strip to horizontal
            val rotatedStrip = Mat()
            Imgproc.warpAffine(
                strip,
                rotatedStrip,
                rotationMatrix,
                src.size(),
                Imgproc.INTER_LINEAR,
                Core.BORDER_CONSTANT,
                Scalar(255.0, 255.0, 255.0)
            )

            // Get the new bounding rectangle after rotation
            val rotatedRect = Imgproc.boundingRect(MatOfPoint(*getRotatedRectPoints(minAreaRect)))

            // Crop the rotated strip
            val croppedStrip = Mat(rotatedStrip, rotatedRect)

            // Resize to standard height
            val resizedStrip = Mat()
            Imgproc.resize(croppedStrip, resizedStrip, Size(src.width().toDouble(), 100.0))

            // Convert to Bitmap
            val outputBitmap = Bitmap.createBitmap(
                resizedStrip.cols(),
                resizedStrip.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(resizedStrip, outputBitmap)

            resultList.add(RedStripData(filteredContours.size, outputBitmap, angle))
        }

        return resultList
    }

    // Helper function to get rotated rectangle points
    private fun getRotatedRectPoints(rect: RotatedRect): Array<Point> {
        val points = Array(4) { Point() }
        rect.points(points)
        return points
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
                binding.tbProcessImage.setTitleTextColor(ContextCompat.getColor(this, R.color.text_primary))
                tintNavigationIcon(navigationIcon, R.color.text_primary)
            }
            Configuration.UI_MODE_NIGHT_YES -> {
                binding.tbProcessImage.setTitleTextColor(ContextCompat.getColor(this, R.color.text_primary))
                tintNavigationIcon(navigationIcon, R.color.text_primary)
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


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.image_process_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_save_image -> {
                saveImagesFromListToPrivateFolder()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveImagesFromListToPrivateFolder() {
        if (::redStripDataList.isInitialized) {
            val privateFolder = getExternalFilesDir(null) // Use getFilesDir() for internal storage
            if (privateFolder != null) {
                redStripDataList.forEach { redStripData ->
                    val fileName = "red_strip_image_${System.currentTimeMillis()}_${redStripData.count}.jpg"
                    val imageFile = File(privateFolder, fileName)

                    try {
                        FileOutputStream(imageFile).use { outputStream ->
                            // Compress the Bitmap and write to file
                            redStripData.bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                        }
//                        Toast.makeText(this, "Saved: $fileName", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, "Failed to save: $fileName", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Private folder not found", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No data to save", Toast.LENGTH_SHORT).show()
        }
    }



}