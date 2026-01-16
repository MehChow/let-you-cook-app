package com.mehchow.letyoucook.data.remote

import com.mehchow.letyoucook.data.model.PresignedUrlBatchRequest
import com.mehchow.letyoucook.data.model.PresignedUrlBatchResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface UploadApiService {
    @POST("api/upload/presigned-urls")
    suspend fun getPresignedUrls(
        @Body request: PresignedUrlBatchRequest
    ): PresignedUrlBatchResponse
}