package com.mehchow.letyoucook.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehchow.letyoucook.data.model.UpdateProfileRequest
import com.mehchow.letyoucook.data.repository.UploadRepository
import com.mehchow.letyoucook.data.repository.UserRepository
import com.mehchow.letyoucook.data.repository.UserResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val uploadRepository: UploadRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    companion object {
        const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L  // 10MB
        val ALLOWED_MIME_TYPES = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")
    }
    
    private val _uiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Loading)
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()
    
    private var originalUsername: String = ""
    
    init {
        loadProfile()
    }
    
    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = EditProfileUiState.Loading
            
            when (val result = userRepository.getCurrentUserProfile()) {
                is UserResult.Success -> {
                    originalUsername = result.data.username
                    _uiState.value = EditProfileUiState.Ready(
                        username = result.data.username,
                        currentAvatarUrl = result.data.avatarUrl
                    )
                }
                is UserResult.Error -> {
                    _uiState.value = EditProfileUiState.Error(result.message)
                }
            }
        }
    }
    
    fun updateUsername(username: String) {
        updateReadyState { it.copy(username = username, errorMessage = null) }
    }
    
    /**
     * Set a new avatar image from local URI
     * Validates file size and type before accepting
     */
    fun setNewAvatar(uri: Uri): String? {
        // Validate file size
        val fileSize = getFileSize(uri)
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            val maxMb = MAX_FILE_SIZE_BYTES / (1024 * 1024)
            return "Image size exceeds ${maxMb}MB limit"
        }
        
        // Validate mime type
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType == null || mimeType.lowercase() !in ALLOWED_MIME_TYPES) {
            return "Invalid image format. Allowed: JPG, PNG, WebP"
        }
        
        updateReadyState { 
            it.copy(
                newAvatarUri = uri, 
                newAvatarR2Key = null,
                errorMessage = null
            ) 
        }
        return null  // No error
    }
    
    fun removeNewAvatar() {
        updateReadyState { 
            it.copy(
                newAvatarUri = null, 
                newAvatarR2Key = null,
                errorMessage = null
            ) 
        }
    }
    
    fun clearError() {
        updateReadyState { it.copy(errorMessage = null) }
    }
    
    /**
     * Save profile changes
     */
    fun saveProfile() {
        val currentState = _uiState.value as? EditProfileUiState.Ready ?: return
        
        if (!currentState.canSave) return
        
        viewModelScope.launch {
            // If there's a new avatar, upload it first
            var avatarR2Key: String? = null
            
            if (currentState.newAvatarUri != null && currentState.newAvatarR2Key == null) {
                updateReadyState { it.copy(isUploading = true, errorMessage = null) }
                
                val uploadResult = uploadAvatar(currentState.newAvatarUri)
                if (uploadResult == null) {
                    updateReadyState { 
                        it.copy(
                            isUploading = false, 
                            errorMessage = "Failed to upload avatar"
                        ) 
                    }
                    return@launch
                }
                avatarR2Key = uploadResult
                updateReadyState { it.copy(isUploading = false, newAvatarR2Key = uploadResult) }
            } else if (currentState.newAvatarR2Key != null) {
                avatarR2Key = currentState.newAvatarR2Key
            }
            
            // Now save the profile
            updateReadyState { it.copy(isSaving = true, errorMessage = null) }
            
            val request = UpdateProfileRequest(
                username = currentState.username.trim(),
                avatarR2Key = avatarR2Key
            )
            
            when (val result = userRepository.updateProfile(request)) {
                is UserResult.Success -> {
                    originalUsername = result.data.username
                    updateReadyState { 
                        it.copy(
                            isSaving = false, 
                            saveSuccess = true,
                            currentAvatarUrl = result.data.avatarUrl,
                            newAvatarUri = null,
                            newAvatarR2Key = null
                        ) 
                    }
                }
                is UserResult.Error -> {
                    updateReadyState { 
                        it.copy(
                            isSaving = false, 
                            errorMessage = result.message
                        ) 
                    }
                }
            }
        }
    }
    
    private suspend fun uploadAvatar(uri: Uri): String? {
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val fileName = "avatar_${System.currentTimeMillis()}.${getExtension(mimeType)}"
        
        // Get presigned URL
        val presignedResult = userRepository.getAvatarPresignedUrl(fileName, mimeType)
        if (presignedResult is UserResult.Error) {
            return null
        }
        
        val presignedUrl = (presignedResult as UserResult.Success).data
        
        // Upload to R2
        val uploadSuccess = uploadRepository.uploadImageToR2(
            uri = uri,
            presignedUrl = presignedUrl,
            contentType = mimeType
        )
        
        return if (uploadSuccess) presignedUrl.r2Key else null
    }
    
    private fun getFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                it.statSize
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun getExtension(mimeType: String): String {
        return when (mimeType.lowercase()) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
    }
    
    private fun updateReadyState(update: (EditProfileUiState.Ready) -> EditProfileUiState.Ready) {
        val current = _uiState.value
        if (current is EditProfileUiState.Ready) {
            _uiState.value = update(current)
        }
    }
}
