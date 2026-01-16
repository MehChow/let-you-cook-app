package com.mehchow.letyoucook.data.remote

import com.mehchow.letyoucook.data.model.AuthRequest
import com.mehchow.letyoucook.data.model.AuthResponse
import com.mehchow.letyoucook.data.model.LogoutRequest
import com.mehchow.letyoucook.data.model.RefreshRequest
import com.mehchow.letyoucook.data.model.RefreshResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/auth/google")
    suspend fun authenticateWithGoogle(
        @Body request: AuthRequest
    ): AuthResponse

    @POST("api/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshRequest
    ): RefreshResponse

    @POST("api/auth/logout")
    suspend fun logout(@Body request: LogoutRequest): Response<Unit>
}