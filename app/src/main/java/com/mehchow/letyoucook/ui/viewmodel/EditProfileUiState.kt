package com.mehchow.letyoucook.ui.viewmodel

import android.net.Uri

/**
 * UI State for the Edit Profile screen
 */
sealed interface EditProfileUiState {
    data object Loading : EditProfileUiState
    
    data class Ready(
        val username: String = "",
        val currentAvatarUrl: String? = null,
        val newAvatarUri: Uri? = null,  // Local URI for new avatar
        val newAvatarR2Key: String? = null,  // R2 key after upload
        val isUploading: Boolean = false,
        val uploadProgress: Float = 0f,
        val isSaving: Boolean = false,
        val errorMessage: String? = null,
        val saveSuccess: Boolean = false
    ) : EditProfileUiState {
        // Validation
        val isUsernameValid: Boolean
            get() = username.isNotBlank()
        
        val canSave: Boolean
            get() = isUsernameValid && !isUploading && !isSaving
        
        // Check if there are unsaved changes
        val hasChanges: Boolean
            get() = newAvatarUri != null  // Username changes tracked separately via original
    }
    
    data class Error(val message: String) : EditProfileUiState
}
