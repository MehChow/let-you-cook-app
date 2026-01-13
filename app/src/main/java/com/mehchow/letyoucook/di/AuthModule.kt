package com.mehchow.letyoucook.di

import com.mehchow.letyoucook.BuildConfig
import com.mehchow.letyoucook.data.local.TokenManager
import com.mehchow.letyoucook.data.remote.AuthApiService
import com.mehchow.letyoucook.data.repository.AuthRepository
import com.mehchow.letyoucook.data.repository.AuthRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
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
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
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
}