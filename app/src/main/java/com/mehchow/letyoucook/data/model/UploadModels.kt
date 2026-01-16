package com.mehchow.letyoucook.data.model

import com.google.gson.annotations.SerializedName

data class PresignedUrlBatchRequest(
    @SerializedName("files")
    val files: List<FileInfo>
)

data class FileInfo(
    @SerializedName("fileName")
    val fileName: String,

    @SerializedName("contentType")
    val contentType: String
)

data class PresignedUrlBatchResponse(
    @SerializedName("uploads")
    val uploads: List<PresignedUrlResponse>
)

data class PresignedUrlResponse(
    @SerializedName("uploadUrl")
    val uploadUrl: String,

    @SerializedName("r2Key")
    val r2Key: String,

    @SerializedName("expiresInSeconds")
    val expiresInSeconds: Int
)