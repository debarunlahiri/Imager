package com.summitcodeworks.imager.apiClient

import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.summitcodeworks.imager.apiInterface.DalleApiService
import com.summitcodeworks.imager.utils.CommonUtils
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://api.openai.com/v1/"

    val client = OkHttpClient.Builder()
        .addInterceptor(ChuckerInterceptor(CommonUtils.mContext))
        .connectTimeout(60, TimeUnit.SECONDS)  // Increase connection timeout
        .readTimeout(60, TimeUnit.SECONDS)     // Increase read timeout
        .writeTimeout(60, TimeUnit.SECONDS)    // Increase write timeout
        .build()



    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: DalleApiService by lazy {
        retrofit.create(DalleApiService::class.java)
    }
}
