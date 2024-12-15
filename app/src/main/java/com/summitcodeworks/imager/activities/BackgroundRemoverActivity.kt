package com.summitcodeworks.imager.activities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.summitcodeworks.imager.R
import com.summitcodeworks.imager.databinding.ActivityBackgroundRemoverBinding
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream

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
            val originalMat = bitmapToMat(bitmap)
            val workingMat = originalMat.clone()

            // Create multiple masks using different techniques
            val grabCutMask = createGrabCutMask(workingMat)
            val kmeansMask = createKMeansMask(workingMat)
            val watershedMask = createWatershedMask(workingMat)
            val featureMask = createFeaturePreservationMask(workingMat)

            // Combine masks using weighted approach
            val combinedMask = combineMasksWithWeights(
                listOf(
                    Pair(grabCutMask, 0.4f),
                    Pair(kmeansMask, 0.2f),
                    Pair(watershedMask, 0.2f),
                    Pair(featureMask, 0.2f)
                )
            )

            // Apply refinement
            val refinedMask = refineSegmentation(workingMat, combinedMask)

            val resultBitmap = applyFinalMask(bitmap, refinedMask)
            binding.imageView.setImageBitmap(resultBitmap)

            // Clean up
            originalMat.release()
            workingMat.release()
            grabCutMask.release()
            kmeansMask.release()
            watershedMask.release()
            featureMask.release()
            combinedMask.release()
            refinedMask.release()

        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Error processing image: ${e.message}")
        }
    }

    private fun combineMasksWithWeights(masks: List<Pair<Mat, Float>>): Mat {
        val result = Mat.zeros(masks[0].first.size(), CvType.CV_32FC1)

        // Combine masks using weights
        masks.forEach { (mask, weight) ->
            val weightedMask = Mat()
            mask.convertTo(weightedMask, CvType.CV_32FC1)
            Core.multiply(weightedMask, Scalar(weight.toDouble()), weightedMask)
            Core.add(result, weightedMask, result)
        }

        // Normalize and convert to binary
        Core.normalize(result, result, 0.0, 255.0, Core.NORM_MINMAX)
        val binaryMask = Mat()
        result.convertTo(binaryMask, CvType.CV_8UC1)
        Imgproc.threshold(binaryMask, binaryMask, 127.0, 255.0, Imgproc.THRESH_BINARY)

        return binaryMask
    }

    private fun refineSegmentation(input: Mat, mask: Mat): Mat {
        val refined = Mat()
        mask.copyTo(refined)

        // Apply guided filter for edge-aware refinement
        val gray = Mat()
        Imgproc.cvtColor(input, gray, Imgproc.COLOR_BGR2GRAY)

        // Convert mask to float
        val guidedMask = Mat()
        mask.convertTo(guidedMask, CvType.CV_32FC1, 1.0/255.0)

        // Apply bilateral filter for edge preservation
        Imgproc.bilateralFilter(guidedMask, refined, 9, 75.0, 75.0)

        // Convert back to binary
        refined.convertTo(refined, CvType.CV_8UC1, 255.0)
        Imgproc.threshold(refined, refined, 127.0, 255.0, Imgproc.THRESH_BINARY)

        return refined
    }


    private fun createGrabCutMask(input: Mat): Mat {
        val mask = Mat()
        val bgModel = Mat()
        val fgModel = Mat()
        val rect = Rect(
            input.cols() / 8,
            input.rows() / 8,
            (input.cols() * 3) / 4,
            (input.rows() * 3) / 4
        )

        // Initialize mask
        mask.create(input.size(), CvType.CV_8UC1)
        mask.setTo(Scalar(Imgproc.GC_PR_BGD.toDouble()))

        // Mark probable foreground
        val probFgMask = createProbableForegroundMask(input)
        mask.setTo(Scalar(Imgproc.GC_PR_FGD.toDouble()), probFgMask)

        // Apply GrabCut
        Imgproc.grabCut(
            input,
            mask,
            rect,
            bgModel,
            fgModel,
            5,
            Imgproc.GC_INIT_WITH_MASK
        )

        // Create binary mask
        val result = Mat()
        Core.compare(mask, Scalar(Imgproc.GC_PR_FGD.toDouble()), result, Core.CMP_EQ)

        return result
    }

    private fun createProbableForegroundMask(input: Mat): Mat {
        val mask = Mat.zeros(input.size(), CvType.CV_8UC1)

        // Convert the image to grayscale
        val gray = Mat()
        Imgproc.cvtColor(input, gray, Imgproc.COLOR_BGR2GRAY)

        // Threshold the grayscale image to create a binary mask (foreground vs background)
        Imgproc.threshold(gray, mask, 50.0, 255.0, Imgproc.THRESH_BINARY)

        // Optional: Apply morphological operations to remove noise and fill gaps in the mask
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0))
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel)

        // Clean up
        gray.release()

        return mask
    }


    private fun createKMeansMask(input: Mat): Mat {
        // Convert to Lab color space for better color segmentation
        val labMat = Mat()
        Imgproc.cvtColor(input, labMat, Imgproc.COLOR_BGR2Lab)

        // Reshape the image to a 2D array of pixels
        val pixels = labMat.reshape(1, (labMat.total()).toInt())

        // Convert to float for k-means
        val floatPixels = Mat()
        pixels.convertTo(floatPixels, CvType.CV_32F)

        // Apply k-means
        val labels = Mat()
        val centers = Mat()
        val criteria = TermCriteria(
            TermCriteria.MAX_ITER + TermCriteria.EPS,
            100,
            0.2
        )

        Core.kmeans(
            floatPixels,
            4, // number of clusters
            labels,
            criteria,
            3,
            Core.KMEANS_PP_CENTERS,
            centers
        )

        // Create mask based on cluster assignment
        val mask = Mat.zeros(input.size(), CvType.CV_8UC1)
        val labelsMat = labels.reshape(1, input.rows())

        // Analyze clusters to determine foreground
        val clusterSizes = IntArray(4)
        for (i in 0 until labelsMat.rows()) {
            for (j in 0 until labelsMat.cols()) {
                clusterSizes[labelsMat.get(i, j)[0].toInt()]++
            }
        }

        // Mark largest cluster as foreground
        val fgCluster = clusterSizes.indexOf(clusterSizes.max())
        for (i in 0 until labelsMat.rows()) {
            for (j in 0 until labelsMat.cols()) {
                if (labelsMat.get(i, j)[0].toInt() == fgCluster) {
                    mask.put(i, j, 255.0)
                }
            }
        }

        return mask
    }

    private fun createWatershedMask(input: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(input, gray, Imgproc.COLOR_BGR2GRAY)

        // Apply threshold
        val thresh = Mat()
        Imgproc.threshold(gray, thresh, 0.0, 255.0, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU)

        // Noise removal
        val kernel = Mat.ones(3, 3, CvType.CV_8UC1)
        val opening = Mat()
        Imgproc.morphologyEx(thresh, opening, Imgproc.MORPH_OPEN, kernel, Point(-1.0, -1.0), 2)

        // Sure background area
        val sureBg = Mat()
        Imgproc.dilate(opening, sureBg, kernel, Point(-1.0, -1.0), 3)

        // Finding sure foreground area
        val distTransform = Mat()
        Imgproc.distanceTransform(opening, distTransform, Imgproc.DIST_L2, 5)
        val sureFg = Mat()
        Core.normalize(distTransform, distTransform, 0.0, 1.0, Core.NORM_MINMAX)
        Imgproc.threshold(distTransform, sureFg, 0.5, 255.0, Imgproc.THRESH_BINARY)

        // Finding unknown region
        val unknown = Mat()
        sureFg.convertTo(sureFg, CvType.CV_8UC1)
        Core.subtract(sureBg, sureFg, unknown)

        // Marker labelling
        val markers = Mat()
        Imgproc.connectedComponents(sureFg, markers)
        Core.add(markers, Scalar(1.0), markers)
        markers.setTo(Scalar(0.0), unknown)

        // Apply watershed
        markers.convertTo(markers, CvType.CV_32SC1)
        Imgproc.watershed(input, markers)

        // Create mask
        val mask = Mat.zeros(input.size(), CvType.CV_8UC1)
        markers.convertTo(markers, CvType.CV_8UC1)
        Core.compare(markers, Scalar(1.0), mask, Core.CMP_GT)

        return mask
    }

    private fun createFeaturePreservationMask(input: Mat): Mat {
        val mask = Mat.zeros(input.size(), CvType.CV_8UC1)

        // Color-based feature detection
        val hsv = Mat()
        Imgproc.cvtColor(input, hsv, Imgproc.COLOR_BGR2HSV)

        // Detect red regions (common in costumes)
        val redMask1 = Mat()
        val redMask2 = Mat()
        Core.inRange(hsv, Scalar(0.0, 70.0, 50.0), Scalar(10.0, 255.0, 255.0), redMask1)
        Core.inRange(hsv, Scalar(170.0, 70.0, 50.0), Scalar(180.0, 255.0, 255.0), redMask2)
        Core.bitwise_or(redMask1, redMask2, redMask1)

        // Detect high contrast regions
        val gray = Mat()
        Imgproc.cvtColor(input, gray, Imgproc.COLOR_BGR2GRAY)
        val edges = Mat()
        Imgproc.Canny(gray, edges, 100.0, 200.0)

        // Combine masks
        Core.bitwise_or(mask, redMask1, mask)
        Core.bitwise_or(mask, edges, mask)

        // Clean up mask
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0))
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel)

        return mask
    }

    private fun detectEdgeFeatures(grayMat: Mat): Mat {
        val edgeMask = Mat()

        // Apply Canny edge detection
        Imgproc.Canny(grayMat, edgeMask, 50.0, 150.0)

        // Dilate edges to create connected regions
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0))
        Imgproc.dilate(edgeMask, edgeMask, kernel)

        return edgeMask
    }

    private fun detectBlobFeatures(grayMat: Mat): Mat {
        val blobMask = Mat.zeros(grayMat.size(), CvType.CV_8UC1)

        // Apply adaptive thresholding to find potential features
        val thresholdMat = Mat()
        Imgproc.adaptiveThreshold(
            grayMat,
            thresholdMat,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            11,
            2.0
        )

        // Find contours in thresholded image
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            thresholdMat,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        // Filter and preserve significant blobs (like eyes)
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > 50 && area < 3000) {  // Adjusted thresholds for eye-sized features
                val boundingRect = Imgproc.boundingRect(contour)
                // Check aspect ratio to filter out non-eye-like shapes
                val aspectRatio = boundingRect.width.toDouble() / boundingRect.height.toDouble()
                if (aspectRatio in 0.5..2.0) {
                    Imgproc.rectangle(blobMask, boundingRect, Scalar(255.0), -1)
                }
            }
            contour.release()
        }

        // Clean up
        thresholdMat.release()
        hierarchy.release()

        return blobMask
    }

    private fun detectContrastFeatures(grayMat: Mat): Mat {
        val contrastMask = Mat.zeros(grayMat.size(), CvType.CV_8UC1)

        // Calculate local contrast
        val blur = Mat()
        Imgproc.GaussianBlur(grayMat, blur, Size(21.0, 21.0), 0.0)
        val contrast = Mat()
        Core.absdiff(grayMat, blur, contrast)

        // Threshold high-contrast regions
        Imgproc.threshold(contrast, contrast, 30.0, 255.0, Imgproc.THRESH_BINARY)

        // Morphological operations to clean up
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0))
        Imgproc.morphologyEx(contrast, contrastMask, Imgproc.MORPH_CLOSE, kernel)

        // Clean up
        blur.release()
        contrast.release()

        return contrastMask
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


    private fun detectAndPreserveFeatures(grayMat: Mat, faceMask: Mat) {
        // Create a mask for high-contrast regions (like Spiderman's eyes)
        val edgeMask = Mat()
        Imgproc.Canny(grayMat, edgeMask, 100.0, 200.0)

        // Dilate edges to create connected regions
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0))
        Imgproc.dilate(edgeMask, edgeMask, kernel)

        // Find contours in edge mask
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            edgeMask,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        // Filter and preserve significant contours
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > 100 && area < 5000) { // Adjust these thresholds based on your needs
                val boundingRect = Imgproc.boundingRect(contour)
                Imgproc.rectangle(faceMask, boundingRect, Scalar(255.0), -1)
            }
            contour.release()
        }

        edgeMask.release()
        hierarchy.release()
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