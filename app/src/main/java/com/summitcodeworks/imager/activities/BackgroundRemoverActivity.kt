package com.summitcodeworks.imager.activities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.summitcodeworks.imager.databinding.ActivityBackgroundRemoverBinding
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.core.Core
import org.opencv.core.Rect
import org.opencv.core.Scalar

class BackgroundRemoverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBackgroundRemoverBinding
    private lateinit var mContext: Context
    private var originalBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackgroundRemoverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mContext = this

        // Image picker
        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                originalBitmap = loadImageFromUri(it)
                originalBitmap?.let { bitmap ->
                    binding.imageView.setImageBitmap(bitmap)
                } ?: run {
                    Toast.makeText(mContext, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.loadImageButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.bRemoveBG.setOnClickListener {
            originalBitmap?.let {
                processImage(it)
            } ?: run {
                Toast.makeText(mContext, "No image loaded", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Load image from URI
    private fun loadImageFromUri(uri: Uri): Bitmap? {
        return try {
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            null
        }
    }

    // Preprocess image: resize and convert to Mat format
    private fun preprocessImage(bitmap: Bitmap): Mat {
        // Convert Bitmap to OpenCV Mat
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Resize the image to 256x256 (you can change this size)
        val resizedMat = Mat()
        Imgproc.resize(mat, resizedMat, Size(256.0, 256.0))

        return resizedMat
    }

    // Create a binary mask (using simple color thresholding)
    private fun createMask(mat: Mat): Mat {
        // Convert the image to HSV color space
        val hsvMat = Mat()
        Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_BGR2HSV)

        // Define lower and upper bounds for the mask (for background removal, adjust these values)
        val lowerBound = Scalar(35.0, 43.0, 46.0) // Example lower bound for green color
        val upperBound = Scalar(85.0, 255.0, 255.0) // Example upper bound for green color

        // Threshold the image to create a binary mask
        val mask = Mat()
        Core.inRange(hsvMat, lowerBound, upperBound, mask)

        return mask
    }

    private fun applyMask(originalBitmap: Bitmap, mask: Mat): Bitmap {
        // Convert the original Bitmap to Mat
        val mat = Mat()
        Utils.bitmapToMat(originalBitmap, mat)

        // Ensure the image is in the correct color format (CV_8UC3)
        if (mat.channels() == 1) {
            // Convert grayscale image to color (BGR)
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_GRAY2BGR)
        } else if (mat.channels() == 4) {
            // Convert RGBA to BGR
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)
        }

        // Resize the mask to match the size of the image (if necessary)
        if (mat.size() != mask.size()) {
            Imgproc.resize(mask, mask, mat.size())
        }

        // Create a grabCut mask for background removal
        val grabCutMask = Mat(mat.size(), CvType.CV_8UC1, Scalar.all(0.0))  // Initialize with zeros

        // Create a background and foreground model for GrabCut
        val bgModel = Mat()
        val fgModel = Mat()

        // Define the rectangle area for GrabCut (this is an approximate region of interest)
        // You can adjust these coordinates based on the specific image
        val rectangle = Rect(50, 50, mat.cols() - 50, mat.rows() - 50) // Approximate rectangle to cover the flower

        // Apply GrabCut algorithm to segment the image
        Imgproc.grabCut(mat, grabCutMask, rectangle, bgModel, fgModel, 5, Imgproc.GC_INIT_WITH_RECT)

        // Create a new mask by setting the foreground pixels
        val newMask = Mat()
        grabCutMask.setTo(Scalar(Imgproc.GC_BGD.toDouble())) // Set all pixels to background by default

        // Now, set all pixels belonging to foreground (GC_FGD and GC_PR_FGD)
        Core.compare(grabCutMask, Scalar(Imgproc.GC_FGD.toDouble()), newMask, Core.CMP_EQ)
        val fgMask = Mat()
        Core.compare(grabCutMask, Scalar(Imgproc.GC_PR_FGD.toDouble()), fgMask, Core.CMP_EQ)
        Core.bitwise_or(newMask, fgMask, newMask) // Combine both foreground masks

        // Apply the mask to the original image
        val resultMat = Mat()
        mat.copyTo(resultMat, newMask) // Apply the refined mask

        // Convert the result Mat back to Bitmap
        val resultBitmap = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(resultMat, resultBitmap)

        return resultBitmap
    }





    // Process the image: Preprocess, create mask, and apply mask
    private fun processImage(bitmap: Bitmap) {
        try {
            // Preprocess the image (resize and convert to Mat)
            val resizedMat = preprocessImage(bitmap)

            // Create a binary mask using OpenCV
            val mask = createMask(resizedMat)

            // Apply the mask to remove the background
            val resultBitmap = applyMask(bitmap, mask)

            // Set the result to the ImageView
            binding.imageView.setImageBitmap(resultBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(mContext, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
