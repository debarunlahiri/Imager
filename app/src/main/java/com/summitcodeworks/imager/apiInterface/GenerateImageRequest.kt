package com.summitcodeworks.imager.apiInterface

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import com.summitcodeworks.imager.BuildConfig


data class GenerateImageRequest(val prompt: String, val n: Int = 1, val size: String = "1024x1024")
data class GenerateImageResponse(val data: List<ImageData>)
data class ImageData(val url: String)


interface DalleApiService {
    @Headers("Authorization: Bearer ${BuildConfig.OPENAI_API_KEY}") // Replace with your OpenAI API key
    @POST("images/generations")
    fun generateImage(@Body request: GenerateImageRequest): Call<GenerateImageResponse>
}