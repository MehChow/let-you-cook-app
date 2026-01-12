package com.mehchow.letyoucook.di

import com.mehchow.letyoucook.data.remote.AuthApiService
import com.mehchow.letyoucook.data.remote.RetrofitClient
import com.mehchow.letyoucook.data.repository.AuthRepository
import com.mehchow.letyoucook.data.repository.AuthRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // These dependencies live for the entire app lifetime
object AuthModule {

    // Provides AuthApiService (the Retrofit interface)
    // Hilt will call this function whenever something needs AuthApiService
    @Provides
    @Singleton // Only create one instance for the entire app
    fun provideAuthApiService(): AuthApiService {
        return RetrofitClient.authApi
    }

    // Provides AuthRepository implementation
    // Hilt will call this function whenever something needs AuthRepository
    // It will inject AuthApiService into AuthRepositoryImpl's constructor
    @Provides
    @Singleton
    fun provideAuthRepository(
        authApiService: AuthApiService
    ): AuthRepository {
        return AuthRepositoryImpl(authApiService)
    }
}