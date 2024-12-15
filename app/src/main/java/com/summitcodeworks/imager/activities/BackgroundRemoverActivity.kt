package com.summitcodeworks.imager.activities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.summitcodeworks.imager.databinding.ActivityBackgroundRemoverBinding
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class BackgroundRemoverActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBackgroundRemoverBinding
    private lateinit var mContext: Context
    private var originalBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackgroundRemoverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mContext = this

        setupImagePicker()
        setupClickListeners()
    }

    private fun setupImagePicker() {
        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                originalBitmap = loadImageFromUri(it)
                originalBitmap?.let { bitmap ->
                    binding.imageView.setImageBitmap(bitmap)
                } ?: showToast("Failed to load image")
            }
        }

        binding.loadImageButton.setOnClickListener {
            pickImage.launch("image/*")
        }
    }

    private fun setupClickListeners() {
        binding.bRemoveBG.setOnClickListener {
            originalBitmap?.let {
                processImageWithAdvancedTechniques(it)
            } ?: showToast("No image loaded")
        }
    }

    private fun loadImageFromUri(uri: Uri): Bitmap? {
        return try {
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Failed to load image")
            null
        }
    }

    private fun processImageWithAdvancedTechniques(bitmap: Bitmap) {
        try {
            // Convert bitmap to Mat
            val originalMat = bitmapToMat(bitmap)

            // Create working copy
            val workingMat = originalMat.clone()

            // Apply preprocessing with error handling
            val preprocessedMat = try {
                applyPreprocessing(workingMat)
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Error in preprocessing: ${e.message}")
                workingMat.clone() // Use original if preprocessing fails
            }

            // Generate initial mask
            val initialMask = generateInitialMask(preprocessedMat)

            // Refine mask
            val refinedMask = refineMask(preprocessedMat, initialMask)

            // Apply final mask
            val resultBitmap = applyFinalMask(bitmap, refinedMask)

            // Display result
            binding.imageView.setImageBitmap(resultBitmap)

            // Clean up
            originalMat.release()
            workingMat.release()
            preprocessedMat.release()
            initialMask.release()
            refinedMask.release()

        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Error processing image: ${e.message}")
        }
    }

    private fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat()
        val tmp = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        Utils.bitmapToMat(tmp, mat)
        // Convert to BGR format which is required for OpenCV processing
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)
        return mat
    }

    private fun applyPreprocessing(input: Mat): Mat {
        val result = Mat()

        // Ensure input is in BGR format for Lab conversion
        val bgrMat = Mat()
        if (input.channels() == 4) {
            Imgproc.cvtColor(input, bgrMat, Imgproc.COLOR_BGRA2BGR)
        } else {
            input.copyTo(bgrMat)
        }

        // Create a separate destination matrix for bilateral filter
        val bilateralResult = Mat()

        // Apply bilateral filter with proper format
        Imgproc.bilateralFilter(bgrMat, bilateralResult, 9, 75.0, 75.0)

        // Convert to Lab color space
        Imgproc.cvtColor(bilateralResult, result, Imgproc.COLOR_BGR2Lab)

        // Enhance contrast using CLAHE
        val labChannels = ArrayList<Mat>()
        Core.split(result, labChannels)

        // Apply CLAHE only to L channel
        val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
        clahe.apply(labChannels[0], labChannels[0])

        // Merge channels back
        Core.merge(labChannels, result)

        // Clean up
        bgrMat.release()
        bilateralResult.release()
        labChannels.forEach { it.release() }

        return result
    }

    private fun generateInitialMask(input: Mat): Mat {
        val mask = Mat()

        // Convert to multiple color spaces for better segmentation
        val hsv = Mat()
        Imgproc.cvtColor(input, hsv, Imgproc.COLOR_Lab2BGR)
        Imgproc.cvtColor(hsv, hsv, Imgproc.COLOR_BGR2HSV)

        // Create masks for different color ranges
        val masks = ArrayList<Mat>()

        // Add different color range masks
        masks.add(createColorRangeMask(hsv, Scalar(0.0, 40.0, 40.0), Scalar(180.0, 255.0, 255.0)))
        masks.add(createSaliencyMask(input))

        // Combine masks
        mask.create(input.size(), CvType.CV_8UC1)
        mask.setTo(Scalar(0.0))

        masks.forEach {
            Core.bitwise_or(mask, it, mask)
        }

        // Apply morphological operations
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0))
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel)

        return mask
    }

    private fun createColorRangeMask(input: Mat, lowerBound: Scalar, upperBound: Scalar): Mat {
        val mask = Mat()
        Core.inRange(input, lowerBound, upperBound, mask)
        return mask
    }

    private fun createSaliencyMask(input: Mat): Mat {
        val saliency = Mat()

        // Convert to grayscale
        Imgproc.cvtColor(input, saliency, Imgproc.COLOR_Lab2BGR)
        Imgproc.cvtColor(saliency, saliency, Imgproc.COLOR_BGR2GRAY)

        // Apply Gaussian blur
        Imgproc.GaussianBlur(saliency, saliency, Size(5.0, 5.0), 0.0)

        // Calculate gradient magnitude
        val gradX = Mat()
        val gradY = Mat()
        Imgproc.Sobel(saliency, gradX, CvType.CV_32F, 1, 0)
        Imgproc.Sobel(saliency, gradY, CvType.CV_32F, 0, 1)

        Core.magnitude(gradX, gradY, saliency)

        // Normalize and threshold
        Core.normalize(saliency, saliency, 0.0, 255.0, Core.NORM_MINMAX)
        saliency.convertTo(saliency, CvType.CV_8UC1)
        Imgproc.threshold(saliency, saliency, 0.0, 255.0, Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU)

        return saliency
    }

    private fun refineMask(input: Mat, mask: Mat): Mat {
        val refined = Mat()
        mask.copyTo(refined)

        // Create grabCut mask
        val grabCutMask = Mat()
        mask.convertTo(grabCutMask, CvType.CV_8UC1)

        // Initialize grabCut masks
        grabCutMask.setTo(Scalar(Imgproc.GC_BGD.toDouble()))
        val fgMask = Mat()
        Core.compare(mask, Scalar(255.0), fgMask, Core.CMP_EQ)
        grabCutMask.setTo(Scalar(Imgproc.GC_FGD.toDouble()), fgMask)

        // Create background and foreground models
        val bgModel = Mat()
        val fgModel = Mat()

        // Apply GrabCut
        Imgproc.cvtColor(input, input, Imgproc.COLOR_Lab2BGR)
        Imgproc.grabCut(input, grabCutMask, Rect(), bgModel, fgModel, 5, Imgproc.GC_INIT_WITH_MASK)

        // Create final mask
        Core.compare(grabCutMask, Scalar(Imgproc.GC_FGD.toDouble()), refined, Core.CMP_EQ)
        val prFgMask = Mat()
        Core.compare(grabCutMask, Scalar(Imgproc.GC_PR_FGD.toDouble()), prFgMask, Core.CMP_EQ)
        Core.bitwise_or(refined, prFgMask, refined)

        // Apply edge-aware refinement
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0))
        Imgproc.morphologyEx(refined, refined, Imgproc.MORPH_OPEN, kernel)
        Imgproc.morphologyEx(refined, refined, Imgproc.MORPH_CLOSE, kernel)

        return refined
    }

    private fun applyFinalMask(originalBitmap: Bitmap, mask: Mat): Bitmap {
        // Convert original bitmap to Mat
        val originalMat = Mat()
        Utils.bitmapToMat(originalBitmap.copy(Bitmap.Config.ARGB_8888, true), originalMat)

        // Create transparent result Mat
        val result = Mat(originalMat.size(), CvType.CV_8UC4, Scalar(0.0, 0.0, 0.0, 0.0))

        // Convert mask to 4 channels
        val maskBGRA = Mat()
        Imgproc.cvtColor(mask, maskBGRA, Imgproc.COLOR_GRAY2BGRA)

        // Copy the foreground pixels
        originalMat.copyTo(result, mask)

        // Convert result back to Bitmap
        val resultBitmap = Bitmap.createBitmap(
            result.cols(), result.rows(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(result, resultBitmap)

        return resultBitmap
    }

    private fun showToast(message: String) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show()
    }
}