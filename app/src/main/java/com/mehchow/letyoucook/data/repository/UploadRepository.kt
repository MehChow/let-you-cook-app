package com.mehchow.letyoucook.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.mehchow.letyoucook.data.model.FileInfo
import com.mehchow.letyoucook.data.model.PresignedUrlBatchRequest
import com.mehchow.letyoucook.data.model.PresignedUrlResponse
import com.mehchow.letyoucook.data.remote.UploadApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

sealed class UploadResult<out T> {
    data class Success<T>(val data: T) : UploadResult<T>()
    data class Error(val message: String) : UploadResult<Nothing>()
}

data class UploadedImage(
    val r2Key: String,
    val localUri: Uri
)

interface UploadRepository {
    suspend fun uploadImages(imageUris: List<Uri>): UploadResult<List<UploadedImage>>
    
    /**
     * Upload a single image to R2 using a presigned URL
     * Used for avatar uploads where presigned URL is obtained separately
     */
    suspend fun uploadImageToR2(
        uri: Uri,
        presignedUrl: PresignedUrlResponse,
        contentType: String
    ): Boolean
}

@Singleton
class UploadRepositoryImpl @Inject constructor(
    private val uploadApiService: UploadApiService,
    private val okHttpClient: OkHttpClient,
    @ApplicationContext private val context: Context
): UploadRepository {
    override suspend fun uploadImages(imageUris: List<Uri>): UploadResult<List<UploadedImage>> {
        return try {
            if (imageUris.isEmpty()) {
                return UploadResult.Error("No images provided")
            }

            // Step 1: Build file info for each image
            val fileInfoList = imageUris.mapIndexed { index, uri ->
                val contentType = getContentType(uri)
                val fileName = getFileName(uri, index)
                FileInfo(fileName = fileName, contentType = contentType)
            }

            // Step 2: Request pre-signed URLs from backend
            val response = uploadApiService.getPresignedUrls(
                PresignedUrlBatchRequest(files = fileInfoList)
            )

            // Step 3: Upload each image to R2
            val uploadedImages = mutableListOf<UploadedImage>()

            imageUris.forEachIndexed { index, uri ->
                val presignedUrl = response.uploads[index]
                val contentType = fileInfoList[index].contentType

                val success = uploadToR2(uri, presignedUrl, contentType)
                if (!success) {
                    return UploadResult.Error("Failed to upload image ${index + 1}")
                }

                uploadedImages.add(
                    UploadedImage(
                        r2Key = presignedUrl.r2Key,
                        localUri = uri
                    )
                )
            }

            UploadResult.Success(uploadedImages)
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                400 -> "Invalid file type. Only JPEG, PNG, and WEBP are allowed."
                401 -> "Session expired. Please log in again."
                429 -> "Too many requests. Please try again later."
                else -> "Upload failed with status code ${e.code()}."
            }
            UploadResult.Error(errorMessage)
        } catch (e: Exception) {
            UploadResult.Error("Upload failed: ${e.localizedMessage}")
        }
    }

    // Plain OkHttpClient for R2 uploads - no auth interceptor, with logging
    private val r2Client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor { message ->
            Log.d("R2_UPLOAD", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        })
        .build()
    
    /**
     * Public method to upload a single image to R2
     */
    override suspend fun uploadImageToR2(
        uri: Uri,
        presignedUrl: PresignedUrlResponse,
        contentType: String
    ): Boolean {
        return uploadToR2(uri, presignedUrl, contentType)
    }
        
    private suspend fun uploadToR2(
        uri: Uri,
        presignedUrl: PresignedUrlResponse,
        contentType: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("R2_UPLOAD", "Starting upload for URI: $uri")
                
                // Read image bytes from the content URI
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: run {
                        Log.e("R2_UPLOAD", "Failed to open input stream for URI: $uri")
                        return@withContext false
                    }
                val bytes = inputStream.readBytes()
                inputStream.close()
                
                Log.d("R2_UPLOAD", "Read ${bytes.size} bytes, uploading to R2...")

                // Create PUT request to R2
                // Note: Only send Content-Type header (must match what was signed)
                // Don't send any extra headers like Authorization or x-amz-content-sha256
                val requestBody = bytes.toRequestBody(contentType.toMediaType())
                val request = Request.Builder()
                    .url(presignedUrl.uploadUrl)
                    .put(requestBody)
                    .header("Content-Type", contentType)
                    .build()

                // Execute the request
                val response = r2Client.newCall(request).execute()
                val success = response.isSuccessful
                
                if (!success) {
                    val errorBody = response.body?.string()
                    Log.e("R2_UPLOAD", "Upload failed: ${response.code} - $errorBody")
                } else {
                    Log.d("R2_UPLOAD", "Upload successful!")
                }
                
                success
            } catch (e: Exception) {
                Log.e("R2_UPLOAD", "Upload exception: ${e.message}", e)
                false
            }
        }
    }

    // Fallback to jpeg when content type is unknown
    private fun getContentType(uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri)
        return when {
            mimeType?.contains("png") == true -> "image/png"
            mimeType?.contains("webp") == true -> "image/webp"
            else -> "image/jpeg"
        }
    }

    private fun getFileName(uri: Uri, index: Int): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    return it.getString(nameIndex)
                }
            }
        }

        // Fallback: Generate filename based on timestamp and index
        return "image_${System.currentTimeMillis()}_$index.jpg"
    }
}