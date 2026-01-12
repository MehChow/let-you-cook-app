package com.mehchow.letyoucook.data.repository

import com.mehchow.letyoucook.data.model.AuthRequest
import com.mehchow.letyoucook.data.model.AuthResponse
import com.mehchow.letyoucook.data.remote.AuthApiService

interface AuthRepository {
    // suspend because it will call the network layer
    suspend fun loginWithGoogle(
        idToken: String
    ): Result<AuthResponse>
}

class AuthRepositoryImpl(
    private val authApiService: AuthApiService
): AuthRepository {
    override suspend fun loginWithGoogle(idToken: String): Result<AuthResponse> {
        // runCatching runs the block and return Result.success(value) or Result.failure(exception)
        return runCatching {
            val request = AuthRequest(idToken = idToken)
            authApiService.authenticateWithGoogle(request)
        }
    }
}