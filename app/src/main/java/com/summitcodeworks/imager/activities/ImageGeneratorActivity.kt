package com.summitcodeworks.imager.activities

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.summitcodeworks.imager.R
import com.summitcodeworks.imager.apiClient.RetrofitClient
import com.summitcodeworks.imager.apiInterface.GenerateImageRequest
import com.summitcodeworks.imager.apiInterface.GenerateImageResponse
import com.summitcodeworks.imager.databinding.ActivityImageGeneratorBinding
import okio.IOException
import org.opencv.BuildConfig
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class ImageGeneratorActivity : AppCompatActivity() {

    private val TAG: String? = ImageGeneratorActivity::class.simpleName
    private lateinit var binding: ActivityImageGeneratorBinding

    private lateinit var mContext: Context
    private lateinit var imageUrl: String
    private lateinit var imageFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageGeneratorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mContext = this

        if (BuildConfig.DEBUG) {
            requestNotificationPermission()
        }

        setupListeners()
    }

    // Function to set up listeners
    private fun setupListeners() {
        binding.btnGenerate.setOnClickListener {
            val prompt = binding.tieDescribeImage.text.toString()
            if (prompt.isNotBlank()) {
                binding.tieDescribeImage.text?.clear()
                hideKeyboard()
                generateImage(prompt)
            } else {
                showToast("Please enter a prompt")
            }
        }

        binding.ibIGSave.setOnClickListener {
            if (this::imageUrl.isInitialized) {
                saveImageToPrivateStorage()
            } else {
                showToast("Generate an image first")
            }
        }

        binding.ibIGShare.setOnClickListener {
            if (::imageUrl.isInitialized && imageFile.exists()) {
                shareImage()
            } else {
                showToast("Save the image first")
            }
        }

        binding.ivResult.setOnClickListener {
            openImagePreview()
        }

        binding.cvIGResult.setOnClickListener {
            onBackPressed()
        }

        binding.ivIGBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus
        view?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun generateImage(prompt: String) {
        val request = GenerateImageRequest(prompt)
        showProgressBar(true)

        RetrofitClient.apiService.generateImage(request).enqueue(object : Callback<GenerateImageResponse> {
            override fun onResponse(call: Call<GenerateImageResponse>, response: Response<GenerateImageResponse>) {
                showProgressBar(false)
                if (response.isSuccessful && response.body() != null) {
                    imageUrl = response.body()!!.data[0].url
                    loadGeneratedImage()
                } else {
                    showToast("Error generating image")
                }
            }

            override fun onFailure(call: Call<GenerateImageResponse>, t: Throwable) {
                showProgressBar(false)
                Log.e(TAG, "onFailure: " + t.message)
                showToast("Network error")
            }
        })
    }

    private fun loadGeneratedImage() {
        Glide.with(this@ImageGeneratorActivity)
            .load(imageUrl)
            .into(binding.ivResult)
    }

    private fun showProgressBar(isVisible: Boolean) {
        binding.progressBar.visibility = if (isVisible) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun saveImageToPrivateStorage() {
        Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    imageFile = File(getExternalFilesDir(null), "${System.currentTimeMillis()}.jpg")
                    try {
                        FileOutputStream(imageFile).use { outputStream ->
                            resource.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                        }
                        showToast("Image saved")
                    } catch (e: IOException) {
                        Log.e(TAG, "Error saving image", e)
                        showToast("Error saving image")
                    }
                }
            })
    }

    private fun shareImage() {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", imageFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share image via"))
    }

    private fun openImagePreview() {
        if (::imageFile.isInitialized) {
            val previewIntent = Intent(this, ImagePreviewActivity::class.java)
            previewIntent.putExtra("imageFilePath", imageFile.absolutePath)
            startActivity(previewIntent)
        }
    }

    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted.")
            } else {
                Log.d(TAG, "Notification permission denied.")
            }
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
}
