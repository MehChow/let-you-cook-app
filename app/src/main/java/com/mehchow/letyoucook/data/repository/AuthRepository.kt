package com.mehchow.letyoucook.data.repository

import com.mehchow.letyoucook.data.local.TokenManager
import com.mehchow.letyoucook.data.model.AuthRequest
import com.mehchow.letyoucook.data.model.AuthResponse
import com.mehchow.letyoucook.data.model.LogoutRequest
import com.mehchow.letyoucook.data.remote.AuthApiService
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

interface AuthRepository {
    // suspend because it will call the network layer
    suspend fun loginWithGoogle(
        idToken: String
    ): Result<AuthResponse>

    suspend fun getCurrentUser(): AuthResponse?

    suspend fun logout(): Result<Unit>
}

class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
): AuthRepository {

    override suspend fun loginWithGoogle(idToken: String): Result<AuthResponse> {
        return runCatching {
            val request = AuthRequest(idToken = idToken)
            val response = authApiService.authenticateWithGoogle(request)
            response // This is the AuthResponse returned by Retrofit
        }.onSuccess { authResponse ->
            // Save to DataStore on success
            tokenManager.saveAuthData(authResponse)
        }
    }

    override suspend fun getCurrentUser(): AuthResponse? {
        // We take a snapshot of the current saved data
        val accessToken = tokenManager.accessToken.firstOrNull()
        val refreshToken = tokenManager.refreshToken.firstOrNull()
        val name = tokenManager.username.firstOrNull()

        return if (accessToken != null && refreshToken != null && name != null) {
            // Reconstruct a partial AuthResponse for the UI to use
            AuthResponse(
                accessToken = accessToken,
                refreshToken = refreshToken, // Add to TokenManager if needed
                email = "",
                username = name,
                avatar = null
            )
        } else {
            null
        }
    }

    override suspend fun logout(): Result<Unit> {
        return runCatching {
            val rt = tokenManager.refreshToken.firstOrNull()
            if (!rt.isNullOrBlank()) {
                authApiService.logout(LogoutRequest(rt))
            }
            // Always clear local data even if API fails
            tokenManager.clearAuthData()
        }
    }
}