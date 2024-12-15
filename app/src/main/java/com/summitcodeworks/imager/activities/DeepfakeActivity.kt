package com.summitcodeworks.imager.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.summitcodeworks.imager.R
import com.summitcodeworks.imager.apiClient.RetrofitClient
import com.summitcodeworks.imager.databinding.ActivityDeepfakeBinding
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class DeepfakeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeepfakeBinding
    private lateinit var selectedImageFile: File
    private var outputVideoUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeepfakeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSelectImage.setOnClickListener {
            openImagePicker()
        }

        binding.btnGenerateDeepfake.setOnClickListener {
            if (::selectedImageFile.isInitialized) {
                generateDeepfake(selectedImageFile)
            } else {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSaveVideo.setOnClickListener {
            if (outputVideoUrl != null) {
                saveVideoToPrivateStorage(outputVideoUrl!!)
            } else {
                Toast.makeText(this, "No video generated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openImagePicker() {
        // Open file picker to select an image
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == IMAGE_PICK_CODE) {
                val imageUri = data.data
                imageUri?.let { uri ->
                    selectedImageFile = File(getRealPathFromURI(uri))
                    Glide.with(this)
                        .load(uri)
                        .into(binding.ivSelectedImage)
                }
            }
        }
    }

    private fun generateDeepfake(imageFile: File) {
        // Show loading indicator
        binding.progressBar.visibility = View.VISIBLE

        // Call your API to generate the deepfake video (you need to implement the backend API)
        val apiService = RetrofitClient.apiService
        val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

//        val call = apiService.createDeepfake(body)
//        call.enqueue(object : Callback<DeepfakeResponse> {
//            override fun onResponse(call: Call<DeepfakeResponse>, response: Response<DeepfakeResponse>) {
//                binding.progressBar.visibility = View.GONE
//                if (response.isSuccessful) {
//                    // Handle successful response
//                    outputVideoUrl = response.body()?.videoUrl
//                    Glide.with(this@DeepfakeActivity)
//                        .load(outputVideoUrl)
//                        .into(binding.ivGeneratedVideoPreview)
//                    Toast.makeText(this@DeepfakeActivity, "Deepfake generated successfully", Toast.LENGTH_SHORT).show()
//                } else {
//                    Toast.makeText(this@DeepfakeActivity, "Error generating deepfake", Toast.LENGTH_SHORT).show()
//                }
//            }
//
//            override fun onFailure(call: Call<DeepfakeResponse>, t: Throwable) {
//                binding.progressBar.visibility = View.GONE
//                Toast.makeText(this@DeepfakeActivity, "Failed to generate deepfake", Toast.LENGTH_SHORT).show()
//            }
//        })
    }

    private fun saveVideoToPrivateStorage(videoUrl: String) {
        val videoFile = File(getExternalFilesDir(null), "generated_video.mp4")
        // You would need to download the video from the provided URL and save it as a file
        // For example, using Glide or Retrofit to download the video content
        Glide.with(this)
            .downloadOnly()
            .load(videoUrl)
            .into(object : SimpleTarget<File>() {
                override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                    // Save the file
                    resource.copyTo(videoFile, overwrite = true)
                    Toast.makeText(this@DeepfakeActivity, "Video saved", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun getRealPathFromURI(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.moveToFirst()
        val columnIndex = cursor?.getColumnIndex(MediaStore.Images.Media.DATA)
        val filePath = cursor?.getString(columnIndex!!)
        cursor?.close()
        return filePath ?: ""
    }

    companion object {
        const val IMAGE_PICK_CODE = 1000
    }
}
