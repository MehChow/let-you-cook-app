package com.mehchow.letyoucook.data.remote

import com.mehchow.letyoucook.data.model.AuthRequest
import com.mehchow.letyoucook.data.model.AuthResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/auth/google")
    suspend fun authenticateWithGoogle(
        @Body request: AuthRequest
    ): AuthResponse

    // You can add these later once we implement them on the backend
    /*
    @POST("api/auth/refresh")
    suspend fun refresh(
        @Body request: RefreshRequest
    ): AuthResponse

    @POST("api/auth/logout")
    suspend fun logout(
        @Body request: RefreshRequest
    ): Void
    */
}