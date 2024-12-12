package com.summitcodeworks.imager

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.summitcodeworks.imager.databinding.ActivityCameraBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var imageReader: ImageReader
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var cameraId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                setupCamera(width, height)
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }

        binding.ibCapture.setOnClickListener {
            captureImage()
        }
    }

    private fun setupCamera(width: Int, height: Int) {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList[0] // Use the first camera
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val largestSize = streamConfigurationMap?.getOutputSizes(ImageFormat.JPEG)?.maxByOrNull { it.width * it.height }

        imageReader = ImageReader.newInstance(largestSize!!.width, largestSize.height, ImageFormat.JPEG, 1)

        // Create a Handler for the callback
        val handlerThread = HandlerThread("CameraBackground").apply { start() }
        val handler = Handler(handlerThread.looper)

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            val buffer: ByteBuffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            saveImageToPrivateFolder(bytes)
            image.close()
        }, handler)
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
            return
        }

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                startPreview()
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
            }
        }, null)
    }

    private fun startPreview() {
        val surfaceTexture = binding.textureView.surfaceTexture
        surfaceTexture?.setDefaultBufferSize(imageReader.width, imageReader.height)
        val previewSurface = Surface(surfaceTexture)
        val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(previewSurface)

        cameraDevice.createCaptureSession(listOf(previewSurface, imageReader.surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Toast.makeText(this@CameraActivity, "Preview Failed", Toast.LENGTH_SHORT).show()
            }
        }, null)
    }

    private fun captureImage() {
        val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureRequestBuilder.addTarget(imageReader.surface)
        captureSession.capture(captureRequestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {}, null)
    }

    private fun saveImageToPrivateFolder(bytes: ByteArray) {
        val fileName = "captured_image_${System.currentTimeMillis()}.jpg"
        val file = File(getExternalFilesDir(null), fileName)

        // Get the camera's sensor orientation and device rotation
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
        val rotation = windowManager.defaultDisplay.rotation

        // Rotate the image based on the device's orientation and the camera's sensor orientation
        val matrix = android.graphics.Matrix()
        when (rotation) {
            Surface.ROTATION_0 -> matrix.postRotate(sensorOrientation.toFloat())
            Surface.ROTATION_90 -> matrix.postRotate((sensorOrientation + 90) % 360.toFloat())
            Surface.ROTATION_180 -> matrix.postRotate((sensorOrientation + 180) % 360.toFloat())
            Surface.ROTATION_270 -> matrix.postRotate((sensorOrientation + 270) % 360.toFloat())
        }

        // Rotate the captured image and save it
        val rotatedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            .let { Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true) }

        // Save the rotated image to the file
        FileOutputStream(file).use { outputStream ->
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }

        // Convert the saved file to a Uri using FileProvider
        val imageUri = FileProvider.getUriForFile(this, "com.summitcodeworks.imager.fileprovider", file)

        Log.d("SaveImage", "File saved at: ${file.absolutePath}")
        Log.d("SaveImage", "File URI: $imageUri")

        // Show a toast message confirming the image has been saved
        runOnUiThread {
            Toast.makeText(this, "Image saved to ${file.absolutePath}", Toast.LENGTH_SHORT).show()

            // Share the image with another activity
            val intent = Intent(this, ImageProcessActivity::class.java)
            intent.putExtra("imageUri", imageUri.toString())  // Pass the Uri
            startActivity(intent)
        }
    }





    override fun onDestroy() {
        super.onDestroy()
        cameraDevice.close()
        imageReader.close()
    }
}
