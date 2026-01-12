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

    @SerializedName("avatarUrl")
    val avatarUrl: String?
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