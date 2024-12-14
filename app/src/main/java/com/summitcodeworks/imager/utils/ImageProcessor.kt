package com.summitcodeworks.imager.utils

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

object ImageProcessor {

    fun enhanceImage(src: Mat, dst: Mat) {
        // Convert source to grayscale
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)

        // Apply Gaussian Blur
        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)

        // Sharpen using Laplacian
        val laplacian = Mat()
        Imgproc.Laplacian(blurred, laplacian, CvType.CV_64F) // Produces CV_64F type

        // Convert Laplacian back to CV_8U
        val laplacian8U = Mat()
        laplacian.convertTo(laplacian8U, CvType.CV_8U)

        // Combine original and sharpened images
        val sharp = Mat()
        Core.addWeighted(gray, 1.5, laplacian8U, -0.5, 0.0, sharp)

        // Convert to 3-channel BGR for output
        Imgproc.cvtColor(sharp, dst, Imgproc.COLOR_GRAY2BGR)
    }
}
