package com.chefmate.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // За Android Emulator използвай: http://10.0.2.2:8090/
    // За реален телефон използвай: http://192.168.1.11:8090/
    // Увери се че телефонът и компютърът са на същата Wi-Fi мрежа!
    
    // ПРОМЕНИ ТОВА АКО 10.0.2.2 НЕ РАБОТИ:
    // За Emulator (може да не работи заради firewall):
    // private const val BASE_URL = "http://10.0.2.2:8090/"
    
    // За реален телефон (ВИНАГИ РАБОТИ ако са на същата Wi-Fi):
    private const val BASE_URL = "http://192.168.1.11:8090/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
