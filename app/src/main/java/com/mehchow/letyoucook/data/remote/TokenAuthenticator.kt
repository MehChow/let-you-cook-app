package com.mehchow.letyoucook.data.remote

import android.util.Log
import com.mehchow.letyoucook.data.local.TokenManager
import com.mehchow.letyoucook.data.model.RefreshRequest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider

/**
 * OkHttp Authenticator that handles 401 Unauthorized responses.
 * 
 * When a request returns 401:
 * 1. This authenticator is called automatically by OkHttp
 * 2. We use the refresh token to get a new access token
 * 3. The original request is retried with the new token
 * 4. If refresh fails, we clear auth data (user needs to re-login)
 * 
 * This is similar to Axios interceptor's response error handler in React.
 * 
 * Note: We use Provider<AuthApiService> to avoid circular dependency,
 * since AuthApiService depends on OkHttpClient which depends on this Authenticator.
 */
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApiServiceProvider: Provider<AuthApiService>
) : Authenticator {

    // Mutex to prevent multiple simultaneous refresh attempts
    private val refreshMutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        Log.d("TokenAuthenticator", "401 received, attempting token refresh...")

        // Don't retry if we've already tried refreshing (prevent infinite loop)
        if (response.request.header("X-Retry-With-Refresh") != null) {
            Log.d("TokenAuthenticator", "Already retried with refresh, giving up")
            return null
        }

        // Don't try to refresh for auth endpoints (avoid infinite loop)
        val requestUrl = response.request.url.toString()
        if (requestUrl.contains("/api/auth/")) {
            Log.d("TokenAuthenticator", "Auth endpoint, skipping refresh")
            return null
        }

        return runBlocking {
            refreshMutex.withLock {
                // Get current tokens
                val currentAccessToken = tokenManager.accessToken.firstOrNull()
                val refreshToken = tokenManager.refreshToken.firstOrNull()

                // Check if the token in the failed request matches current token
                // If not, another thread may have already refreshed it
                val requestToken = response.request.header("Authorization")
                    ?.removePrefix("Bearer ")

                if (requestToken != null && requestToken != currentAccessToken && currentAccessToken != null) {
                    // Token was already refreshed by another request, just retry with new token
                    Log.d("TokenAuthenticator", "Token already refreshed, retrying with new token")
                    return@runBlocking response.request.newBuilder()
                        .header("Authorization", "Bearer $currentAccessToken")
                        .header("X-Retry-With-Refresh", "true")
                        .build()
                }

                // No refresh token available - user needs to re-login
                if (refreshToken.isNullOrBlank()) {
                    Log.d("TokenAuthenticator", "No refresh token available, clearing auth")
                    tokenManager.clearAuthData()
                    return@runBlocking null
                }

                try {
                    // Call refresh endpoint
                    Log.d("TokenAuthenticator", "Calling refresh endpoint...")
                    val authApiService = authApiServiceProvider.get()
                    val refreshResponse = authApiService.refreshToken(RefreshRequest(refreshToken))

                    // Save new access token
                    val newAccessToken = refreshResponse.accessToken
                    tokenManager.updateAccessToken(newAccessToken)
                    Log.d("TokenAuthenticator", "Token refreshed successfully")

                    // Retry original request with new token
                    return@runBlocking response.request.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .header("X-Retry-With-Refresh", "true")
                        .build()

                } catch (e: Exception) {
                    Log.e("TokenAuthenticator", "Token refresh failed: ${e.message}")
                    // Refresh failed - clear auth data so user can re-login
                    tokenManager.clearAuthData()
                    return@runBlocking null
                }
            }
        }
    }
}
