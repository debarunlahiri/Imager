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
        // Get the crop rectangle from CropView
        val cropRect = binding.cropView.getCropRect()

        // Check if the cropRect is a RectF and convert it to Rect
        if (cropRect is RectF) {
            val scaleX = originalBitmap.width / binding.cropView.width.toFloat()
            val scaleY = originalBitmap.height / binding.cropView.height.toFloat()

            // Convert RectF to Rect by scaling
            val scaledCropRect = RectF(
                (cropRect.left * scaleX).toInt().toFloat(),
                (cropRect.top * scaleY).toInt().toFloat(),
                (cropRect.right * scaleX).toInt().toFloat(),
                (cropRect.bottom * scaleY).toInt().toFloat()
            )

            // Ensure the crop rectangle is within bounds
            val clampedCropRect = RectF(
                scaledCropRect.left.coerceAtLeast(0F),
                scaledCropRect.top.coerceAtLeast(0F),
                scaledCropRect.right.coerceAtMost(originalBitmap.width.toFloat()),
                scaledCropRect.bottom.coerceAtMost(originalBitmap.height.toFloat())
            )

            // Crop the bitmap using the clamped rectangle
            val croppedBitmap = Bitmap.createBitmap(
                originalBitmap,
                clampedCropRect.left.toInt(),
                clampedCropRect.top.toInt(),
                clampedCropRect.width().toInt(),
                clampedCropRect.height().toInt()
            )

            // Update the original bitmap and the ImageView
            originalBitmap = croppedBitmap
            binding.imageView.setImageBitmap(croppedBitmap)

            Toast.makeText(this, "Image cropped", Toast.LENGTH_SHORT).show()

            // Hide the CropView after cropping
            binding.cropView.visibility = View.GONE
        } else {
            Toast.makeText(this, "Failed to get crop rectangle", Toast.LENGTH_SHORT).show()
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

        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(grayMat, grayMat, Size(5.0, 5.0), 0.0)

        val edges = Mat()
        Imgproc.Canny(grayMat, edges, 75.0, 200.0)

        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(edges, contours, Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

        val largestContour = contours.maxByOrNull { Imgproc.contourArea(it) }
        if (largestContour != null && Imgproc.contourArea(largestContour) > 5000) {
            val approx = MatOfPoint2f()
            val peri = Imgproc.arcLength(MatOfPoint2f(*largestContour.toArray()), true)
            Imgproc.approxPolyDP(MatOfPoint2f(*largestContour.toArray()), approx, 0.02 * peri, true)

            if (approx.total() == 4L) {
                corners.clear()
                corners.addAll(approx.toList())
                drawCorners(mat)
                Toast.makeText(this, "Document detected", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Unable to detect a quadrilateral", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No significant contours found", Toast.LENGTH_SHORT).show()
        }

        val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, resultBitmap)
        binding.imageView.setImageBitmap(resultBitmap)
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
        Imgproc.warpPerspective(originalMat, transformedMat, transformMat, Size(width.toDouble(), height.toDouble()))

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(transformedMat, resultBitmap)
        return resultBitmap
    }

    private fun orderPoints(points: List<Point>): List<Point> {
        val sorted = points.sortedBy { it.x + it.y }
        val topLeft = sorted[0]
        val bottomRight = sorted[3]

        val remaining = sorted.subList(1, 3).sortedBy { it.x - it.y }
        val topRight = remaining[0]
        val bottomLeft = remaining[1]

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
