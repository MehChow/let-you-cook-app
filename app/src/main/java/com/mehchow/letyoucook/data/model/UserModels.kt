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