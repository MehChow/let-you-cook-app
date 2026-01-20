package com.mehchow.letyoucook.data.repository

import com.mehchow.letyoucook.data.model.AvatarUploadRequest
import com.mehchow.letyoucook.data.model.PresignedUrlResponse
import com.mehchow.letyoucook.data.model.UpdateProfileRequest
import com.mehchow.letyoucook.data.model.UserProfile
import com.mehchow.letyoucook.data.remote.UserApiService
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

sealed class UserResult<out T> {
    data class Success<T>(val data: T): UserResult<T>()
    data class Error(val message: String): UserResult<Nothing>()
}

interface UserRepository {
    suspend fun getCurrentUserProfile(): UserResult<UserProfile>
    suspend fun updateProfile(request: UpdateProfileRequest): UserResult<UserProfile>
    suspend fun getAvatarPresignedUrl(fileName: String, contentType: String): UserResult<PresignedUrlResponse>
}

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userApiService: UserApiService
): UserRepository {
    override suspend fun getCurrentUserProfile(): UserResult<UserProfile> {
        return safeApiCall {
            userApiService.getCurrentUserProfile()
        }
    }
    
    override suspend fun updateProfile(request: UpdateProfileRequest): UserResult<UserProfile> {
        return safeApiCall {
            userApiService.updateProfile(request)
        }
    }
    
    override suspend fun getAvatarPresignedUrl(
        fileName: String, 
        contentType: String
    ): UserResult<PresignedUrlResponse> {
        return safeApiCall {
            userApiService.getAvatarPresignedUrl(
                AvatarUploadRequest(fileName, contentType)
            )
        }
    }

    private suspend fun <T> safeApiCall(block: suspend () -> T): UserResult<T> {
        return try {
            UserResult.Success(block())
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                400 -> "Invalid request. Please check your input."
                401 -> "Session expired. Please login again"
                403 -> "Access denied"
                404 -> "User not found"
                else -> "Server error: ${e.message}"
            }
            UserResult.Error(errorMessage)
        } catch (e: UnknownHostException) {
            UserResult.Error("No internet connection")
        } catch (e: SocketTimeoutException) {
            UserResult.Error("Request timed out")
        } catch (e: Exception) {
            UserResult.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }
}