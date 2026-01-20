package com.mehchow.letyoucook.data.model

import com.google.gson.annotations.SerializedName

data class UserProfile(
    @SerializedName("id")
    val id: Long,

    @SerializedName("username")
    val username: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("avatarUrl")
    val avatarUrl: String?,

    @SerializedName("createdAt")
    val createdAt: String,
)

/**
 * Request to update user profile
 */
data class UpdateProfileRequest(
    @SerializedName("username")
    val username: String,
    
    @SerializedName("avatarR2Key")
    val avatarR2Key: String? = null
)

/**
 * Request to get avatar upload presigned URL
 */
data class AvatarUploadRequest(
    @SerializedName("fileName")
    val fileName: String,
    
    @SerializedName("contentType")
    val contentType: String
)