package com.mehchow.letyoucook.data.model

import com.google.gson.annotations.SerializedName

/**
 * Request object sent to /api/auth/google
 */
data class AuthRequest(
    @SerializedName("idToken")
    val idToken: String
)

/**
 * Success response received from the backend
 */
data class AuthResponse(
    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("refreshToken")
    val refreshToken: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("avatar")
    val avatar: String?
)

data class LogoutRequest(
    @SerializedName("refreshToken")
    val refreshToken: String
)

/**
 * Request object sent to /api/auth/refresh
 */
data class RefreshRequest(
    @SerializedName("refreshToken")
    val refreshToken: String
)

/**
 * Response from /api/auth/refresh
 */
data class RefreshResponse(
    @SerializedName("accessToken")
    val accessToken: String
)

/**
 * Generic error response to match your GlobalExceptionHandler
 */
data class ErrorResponse(
    @SerializedName("errorCode")
    val errorCode: String,

    @SerializedName("message")
    val message: String
)