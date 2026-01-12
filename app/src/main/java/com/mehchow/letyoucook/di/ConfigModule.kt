package com.mehchow.letyoucook.di

import com.mehchow.letyoucook.BuildConfig

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    @Provides
    @Singleton
    @Named("web_client_id")
    fun provideWebClientId(): String {
        // This pulls the value we defined in build.gradle.kts / local.properties
        return BuildConfig.GOOGLE_WEB_CLIENT_ID
    }
}