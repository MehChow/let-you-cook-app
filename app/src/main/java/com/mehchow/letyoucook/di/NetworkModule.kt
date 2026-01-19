package com.mehchow.letyoucook.di

import android.content.Context
import com.mehchow.letyoucook.data.local.TokenManager
import com.mehchow.letyoucook.data.remote.AuthInterceptor
import com.mehchow.letyoucook.data.remote.RecipeApiService
import com.mehchow.letyoucook.data.remote.UploadApiService
import com.mehchow.letyoucook.data.remote.UserApiService
import com.mehchow.letyoucook.data.repository.RecipeRepository
import com.mehchow.letyoucook.data.repository.RecipeRepositoryImpl
import com.mehchow.letyoucook.data.repository.UploadRepository
import com.mehchow.letyoucook.data.repository.UploadRepositoryImpl
import com.mehchow.letyoucook.data.repository.UserRepository
import com.mehchow.letyoucook.data.repository.UserRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Provides AuthInterceptor for adding JWT tokens to requests.
     * This is the single source for AuthInterceptor in the app.
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor {
        return AuthInterceptor(tokenManager)
    }

    @Provides
    @Singleton
    fun provideRecipeApiService(retrofit: Retrofit): RecipeApiService {
        return retrofit.create(RecipeApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideUploadApiService(retrofit: Retrofit): UploadApiService {
        return retrofit.create(UploadApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRecipeRepository(
        recipeApiService: RecipeApiService
    ): RecipeRepository {
        return RecipeRepositoryImpl(recipeApiService)
    }

    @Provides
    @Singleton
    fun provideUploadRepository(
        uploadApiService: UploadApiService,
        okHttpClient: OkHttpClient,
        @ApplicationContext context: Context
    ): UploadRepository {
        return UploadRepositoryImpl(uploadApiService, okHttpClient, context)
    }

    @Provides
    @Singleton
    fun provideUserApiService(retrofit: Retrofit): UserApiService {
        return retrofit.create(UserApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        userApiService: UserApiService
    ): UserRepository {
        return UserRepositoryImpl(userApiService)
    }
}