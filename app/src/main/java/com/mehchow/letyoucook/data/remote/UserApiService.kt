package com.mehchow.letyoucook.data.remote

import com.mehchow.letyoucook.data.model.AvatarUploadRequest
import com.mehchow.letyoucook.data.model.PresignedUrlResponse
import com.mehchow.letyoucook.data.model.UpdateProfileRequest
import com.mehchow.letyoucook.data.model.UserProfile
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface UserApiService {
    @GET("api/users/me")
    suspend fun getCurrentUserProfile(): UserProfile
    
    @PUT("api/users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): UserProfile
    
    @POST("api/users/me/avatar/presigned-url")
    suspend fun getAvatarPresignedUrl(@Body request: AvatarUploadRequest): PresignedUrlResponse
}