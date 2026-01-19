package com.mehchow.letyoucook.data.remote

import com.mehchow.letyoucook.data.model.UserProfile
import retrofit2.http.GET

interface UserApiService {
    @GET("api/users/me")
    suspend fun getCurrentUserProfile(): UserProfile
}