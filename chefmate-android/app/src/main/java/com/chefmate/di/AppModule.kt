package com.chefmate.di

import com.chefmate.data.api.ApiClient
import com.chefmate.data.api.ApiService

object AppModule {
    fun provideApiService(): ApiService {
        return ApiClient.apiService
    }
}

