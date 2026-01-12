package com.mehchow.letyoucook.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // If using 'adb reverse tcp:8080 tcp:8080', use localhost:
    private const val BASE_URL = "http://127.0.0.1:8080/"

    val authApi: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }
}