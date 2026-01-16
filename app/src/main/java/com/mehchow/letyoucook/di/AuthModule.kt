package com.mehchow.letyoucook.di

import android.util.Log
import com.google.gson.GsonBuilder
import com.mehchow.letyoucook.BuildConfig
import com.mehchow.letyoucook.data.local.TokenManager
import com.mehchow.letyoucook.data.remote.AuthApiService
import com.mehchow.letyoucook.data.remote.AuthInterceptor
import com.mehchow.letyoucook.data.repository.AuthRepository
import com.mehchow.letyoucook.data.repository.AuthRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // These dependencies live for the entire app lifetime
object AuthModule {

    // Provides AuthApiService (the Retrofit interface)
    // Hilt will call this function whenever something needs AuthApiService
    @Provides
    @Singleton // Only create one instance for the entire app
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        @Named("base_url") baseUrl: String
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(createLoggingInterceptor())
                }
            }
            .build()
    }

    @Provides
    @Singleton
    @Named("base_url")
    fun provideBaseUrl(): String {
        return BuildConfig.BASE_URL
    }

    // Provides AuthRepository implementation
    // Hilt will call this function whenever something needs AuthRepository
    // It will inject AuthApiService into AuthRepositoryImpl's constructor
    @Provides
    @Singleton
    fun provideAuthRepository(
        authApiService: AuthApiService,
        tokenManager: TokenManager
    ): AuthRepository {
        return AuthRepositoryImpl(authApiService, tokenManager)
    }

    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            Log.d("API_COOKING!!", prettyPrintJson(message))
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private fun prettyPrintJson(message: String): String {
        return try {
            if (message.startsWith("{") || message.startsWith("[")) {
                val gson = GsonBuilder().setPrettyPrinting().create()
                val json = gson.fromJson(message, Any::class.java)
                gson.toJson(json)
            } else {
                message
            }
        } catch (e: Exception) {
            message
        }
    }
}