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
import org.opencv.photo.Photo

class DocumentScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDocumentScanBinding
    private lateinit var mContext: Context
    private lateinit var originalBitmap: Bitmap
    private val corners = mutableListOf<Point>()

    private val MIN_DOCUMENT_AREA_PERCENT = 0.10 // Reduced from 0.15
    private val MAX_DOCUMENT_AREA_PERCENT = 0.98 // Increased from 0.95

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

        // Enhanced pre-processing
        val processed = preprocessImage(mat)

        // Find document corners
        val documentCorners = findDocumentCorners(processed)

        if (documentCorners != null) {
            corners.clear()
            corners.addAll(documentCorners)

            // Apply perspective transform
            val transformedBitmap = perspectiveTransform()
            originalBitmap = transformedBitmap
            binding.imageView.setImageBitmap(transformedBitmap)

            Toast.makeText(this, "Document detected", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No document detected", Toast.LENGTH_SHORT).show()
            binding.imageView.setImageBitmap(bitmap)
        }

        mat.release()
        processed.release()
    }

    private fun preprocessImage(input: Mat): Mat {
        val result = Mat()

        // Convert to grayscale
        Imgproc.cvtColor(input, result, Imgproc.COLOR_BGR2GRAY)

        // Increase contrast
        Core.normalize(result, result, 0.0, 255.0, Core.NORM_MINMAX)

        // Apply bilateral filter with adjusted parameters
        Photo.fastNlMeansDenoising(result, result, 10f)

        // Use less aggressive CLAHE
        val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
        clahe.apply(result, result)

        // Lighter Gaussian blur
        Imgproc.GaussianBlur(result, result, Size(3.0, 3.0), 1.0)

        // Remove adaptive thresholding as it might be too aggressive
        // Instead, use a simple threshold with Otsu's method
        Imgproc.threshold(result, result, 0.0, 255.0, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU)

        return result
    }

    private fun findDocumentCorners(processed: Mat): List<Point>? {
        val edges = Mat()
        val hierarchy = Mat()

        // Multi-scale edge detection
        val edgesMat = detectEdgesMultiScale(processed)

        // Find contours
        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(
            edgesMat,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        // Sort contours by area in descending order
        val sortedContours = contours
            .filter { validateContourArea(it, processed.size()) }
            .sortedByDescending { Imgproc.contourArea(it) }

        for (contour in sortedContours) {
            val approx = approximatePolygon(contour)

            if (approx.total() == 4L && isValidQuadrilateral(approx)) {
                val points = approx.toList()
                hierarchy.release()
                edges.release()
                edgesMat.release()
                return points
            }
        }

        hierarchy.release()
        edges.release()
        edgesMat.release()
        return null
    }

    private fun detectEdgesMultiScale(gray: Mat): Mat {
        val edges = Mat()
        val edgesTemp = Mat()

        // Add more scales for better detection
        val scales = listOf(1.0, 0.75, 0.5, 0.25)

        for (scale in scales) {
            val scaled = Mat()
            val size = Size(gray.width() * scale, gray.height() * scale)
            Imgproc.resize(gray, scaled, size)

            // Adjust Canny parameters
            val sigma = 0.33
            val median = calculateMedian(scaled)
            // Lower thresholds for more sensitive edge detection
            val lower = (median * (1.0 - sigma)).coerceAtLeast(0.0) * 0.5 // Reduced threshold
            val upper = (median * (1.0 + sigma)).coerceAtMost(255.0)

            Imgproc.Canny(scaled, edgesTemp, lower, upper)

            if (scale != 1.0) {
                Imgproc.resize(edgesTemp, edgesTemp, gray.size())
            }

            if (edges.empty()) {
                edgesTemp.copyTo(edges)
            } else {
                Core.bitwise_or(edges, edgesTemp, edges)
            }

            scaled.release()
        }

        // More aggressive edge enhancement
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
        Imgproc.dilate(edges, edges, kernel)
        Imgproc.erode(edges, edges, kernel)

        edgesTemp.release()
        return edges
    }

    private fun calculateMedian(mat: Mat): Double {
        val values = MatOfDouble()
        Core.meanStdDev(mat, values, MatOfDouble())
        return values.get(0, 0)[0]
    }

    private fun validateContourArea(contour: MatOfPoint, imageSize: Size): Boolean {
        val area = Imgproc.contourArea(contour)
        val imageArea = imageSize.area()
        val areaRatio = area / imageArea

        return areaRatio in MIN_DOCUMENT_AREA_PERCENT..MAX_DOCUMENT_AREA_PERCENT
    }

    private fun approximatePolygon(contour: MatOfPoint): MatOfPoint2f {
        val contour2f = MatOfPoint2f(*contour.toArray())
        val approx = MatOfPoint2f()
        // Increase epsilon for more lenient approximation
        val epsilon = 0.03 * Imgproc.arcLength(contour2f, true)  // Increased from 0.02
        Imgproc.approxPolyDP(contour2f, approx, epsilon, true)
        return approx
    }

    private fun isValidQuadrilateral(points: MatOfPoint2f): Boolean {
        if (points.total() != 4L) return false

        val vertices = points.toList()

        // Reduce minimum angle requirement
        val minAngle = calculateMinimumAngle(vertices)
        if (minAngle < 30.0) return false  // Reduced from 45.0

        // More lenient aspect ratio
        val aspectRatio = calculateAspectRatio(vertices)
        if (aspectRatio < 0.1 || aspectRatio > 10.0) return false  // Widened from 0.2-5.0

        return true
    }

    private fun calculateMinimumAngle(points: List<Point>): Double {
        var minAngle = 180.0

        for (i in points.indices) {
            val p1 = points[i]
            val p2 = points[(i + 1) % 4]
            val p3 = points[(i + 2) % 4]

            val angle = calculateAngle(p1, p2, p3)
            minAngle = minOf(minAngle, angle)
        }

        return minAngle
    }

    private fun calculateAngle(p1: Point, p2: Point, p3: Point): Double {
        val v1x = p1.x - p2.x
        val v1y = p1.y - p2.y
        val v2x = p3.x - p2.x
        val v2y = p3.y - p2.y

        val dot = v1x * v2x + v1y * v2y
        val cross = v1x * v2y - v1y * v2x

        return Math.toDegrees(Math.atan2(Math.abs(cross), dot))
    }

    private fun calculateAspectRatio(points: List<Point>): Double {
        val width = maxOf(
            distance(points[0], points[1]),
            distance(points[2], points[3])
        )

        val height = maxOf(
            distance(points[0], points[3]),
            distance(points[1], points[2])
        )

        return width / height
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
