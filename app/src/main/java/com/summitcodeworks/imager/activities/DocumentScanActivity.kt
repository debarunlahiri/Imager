package com.summitcodeworks.imager.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.summitcodeworks.imager.R
import com.summitcodeworks.imager.databinding.ActivityDocumentScanBinding
import com.summitcodeworks.imager.utils.CommonUtils
import com.summitcodeworks.imager.utils.CropView
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class DocumentScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDocumentScanBinding
    private lateinit var mContext: Context
    private lateinit var originalBitmap: Bitmap
    private val corners = mutableListOf<Point>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mContext = this

        binding.cropView.visibility = View.GONE

        launchCamera()

        binding.btnRotateLeft.setOnClickListener {
            originalBitmap = rotateImage(originalBitmap, -90f)
            binding.imageView.setImageBitmap(originalBitmap)
        }

        binding.btnRotateRight.setOnClickListener {
            originalBitmap = rotateImage(originalBitmap, 90f)
            binding.imageView.setImageBitmap(originalBitmap)
        }

        binding.btnCrop.setOnClickListener {
            if (binding.cropView.isVisible) {
                cropImage()
            } else {
                binding.cropView.visibility = View.VISIBLE
            }
        }
    }

    private fun cropImage() {
        val cropRect = binding.cropView.getCropRect()

        // Get the actual dimensions of the ImageView
        val imageView = binding.imageView
        val imageViewWidth = imageView.width.toFloat()
        val imageViewHeight = imageView.height.toFloat()

        // Calculate scaling factors
        val scaleX = originalBitmap.width / imageViewWidth
        val scaleY = originalBitmap.height / imageViewHeight

        // Scale the crop rectangle to match the original bitmap dimensions
        val scaledRect = RectF(
            cropRect.left * scaleX,
            cropRect.top * scaleY,
            cropRect.right * scaleX,
            cropRect.bottom * scaleY
        )

        // Ensure the scaled rectangle is within bounds
        val clampedRect = RectF(
            scaledRect.left.coerceIn(0f, originalBitmap.width.toFloat()),
            scaledRect.top.coerceIn(0f, originalBitmap.height.toFloat()),
            scaledRect.right.coerceIn(0f, originalBitmap.width.toFloat()),
            scaledRect.bottom.coerceIn(0f, originalBitmap.height.toFloat())
        )

        try {
            val croppedBitmap = Bitmap.createBitmap(
                originalBitmap,
                clampedRect.left.toInt(),
                clampedRect.top.toInt(),
                (clampedRect.right - clampedRect.left).toInt(),
                (clampedRect.bottom - clampedRect.top).toInt()
            )

            originalBitmap = croppedBitmap
            binding.imageView.setImageBitmap(croppedBitmap)
            binding.cropView.visibility = View.GONE

            Toast.makeText(this, "Image cropped successfully", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "Failed to crop image", Toast.LENGTH_SHORT).show()
            Log.e("DocumentScanActivity", "Crop failed: ${e.message}")
        }
    }



    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.getStringExtra(CameraActivity.EXTRA_IMAGE_URI)

            if (imageUri != null) {
                val uri = Uri.parse(imageUri)
                originalBitmap = CommonUtils.convertUriToBitmap(this, uri)!!
                binding.imageView.setImageURI(uri)
                processImage(originalBitmap)
            } else {
                Toast.makeText(this, "Failed to get image URI", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Image capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        cameraLauncher.launch(intent)
    }

    private fun processImage(bitmap: Bitmap) {
        originalBitmap = bitmap
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convert to BGR for OpenCV processing
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2BGR)

        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(grayMat, grayMat, Size(5.0, 5.0), 0.0)

        val edges = Mat()
        // Adjusted threshold values for better edge detection
        Imgproc.Canny(grayMat, edges, 50.0, 150.0)

        // Dilate edges to connect broken lines
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        Imgproc.dilate(edges, edges, kernel)

        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        // Sort contours by area in descending order
        val sortedContours = contours.sortedByDescending { Imgproc.contourArea(it) }

        var documentDetected = false

        for (contour in sortedContours) {
            val area = Imgproc.contourArea(contour)
            // Check if contour is large enough (adjust threshold as needed)
            if (area < 0.05 * mat.width() * mat.height()) continue

            val approx = MatOfPoint2f()
            val peri = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
            Imgproc.approxPolyDP(MatOfPoint2f(*contour.toArray()), approx, 0.02 * peri, true)

            if (approx.total() == 4L) {
                corners.clear()
                corners.addAll(approx.toList())
                drawCorners(mat)
                documentDetected = true

                // Apply perspective transform
                val transformedBitmap = perspectiveTransform()
                originalBitmap = transformedBitmap
                binding.imageView.setImageBitmap(transformedBitmap)

                Toast.makeText(this, "Document detected", Toast.LENGTH_SHORT).show()
                break
            }
        }

        if (!documentDetected) {
            Toast.makeText(this, "No document detected", Toast.LENGTH_SHORT).show()
            binding.imageView.setImageBitmap(bitmap)
        }

        mat.release()
        grayMat.release()
        edges.release()
        hierarchy.release()
    }

    private fun drawCorners(mat: Mat) {
        corners.forEach { point ->
            Imgproc.circle(mat, point, 10, Scalar(0.0, 255.0, 0.0), -1)
        }
    }

    private fun perspectiveTransform(): Bitmap {
        if (corners.size != 4) {
            Toast.makeText(this, "Invalid corner count for transform", Toast.LENGTH_SHORT).show()
            return originalBitmap
        }

        val orderedPoints = orderPoints(corners)
        val width = maxOf(
            distance(orderedPoints[0], orderedPoints[1]),
            distance(orderedPoints[2], orderedPoints[3])
        ).toInt()

        val height = maxOf(
            distance(orderedPoints[0], orderedPoints[3]),
            distance(orderedPoints[1], orderedPoints[2])
        ).toInt()

        val src = MatOfPoint2f(*orderedPoints.toTypedArray())
        val dst = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(width.toDouble(), 0.0),
            Point(width.toDouble(), height.toDouble()),
            Point(0.0, height.toDouble())
        )

        val transformMat = Imgproc.getPerspectiveTransform(src, dst)
        val transformedMat = Mat()
        val originalMat = Mat()
        Utils.bitmapToMat(originalBitmap, originalMat)

        // Fix potential flipping by ensuring the transformation matrix is applied correctly
        Imgproc.warpPerspective(
            originalMat,
            transformedMat,
            transformMat,
            Size(width.toDouble(), height.toDouble()),
            Imgproc.INTER_LINEAR,  // Use linear interpolation for better quality
            Core.BORDER_CONSTANT,
            Scalar(0.0, 0.0, 0.0) // Optional: Fill borders with black if needed
        )

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(transformedMat, resultBitmap)

        // Release the matrices to free memory
        originalMat.release()
        transformedMat.release()

        return resultBitmap
    }


    private fun orderPoints(points: List<Point>): List<Point> {
        // Find top-left (smallest sum), bottom-right (largest sum)
        val sortedSum = points.sortedBy { it.x + it.y }
        val topLeft = sortedSum[0]
        val bottomRight = sortedSum[3]

        // Find top-right (smallest difference), bottom-left (largest difference)
        val sortedDiff = points.sortedBy { it.y - it.x }
        val topRight = sortedDiff[0]
        val bottomLeft = sortedDiff[3]

        return listOf(topLeft, topRight, bottomRight, bottomLeft)
    }

    private fun distance(p1: Point, p2: Point): Double {
        return Math.hypot(p2.x - p1.x, p2.y - p1.y)
    }

    private fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
