package com.mehchow.letyoucook.ui.viewmodel

import android.net.Uri
import com.mehchow.letyoucook.data.model.RecipeVisibility
import java.util.UUID

/**
 * Represents an image in the edit form.
 * Can be either an existing image from R2 or a new local image.
 */
sealed class EditableImage {
    abstract val id: String
    abstract val displayOrder: Int
    
    /**
     * An existing image already uploaded to R2.
     */
    data class Existing(
        override val id: String,
        val imageUrl: String,
        val r2Key: String,  // Extracted from URL for update request
        override val displayOrder: Int
    ) : EditableImage()
    
    /**
     * A new image selected from the device.
     */
    data class New(
        override val id: String = UUID.randomUUID().toString(),
        val uri: Uri,
        override val displayOrder: Int,
        val uploadProgress: Float = 0f,
        val isUploaded: Boolean = false,
        val r2Key: String? = null
    ) : EditableImage()
}

/**
 * Represents a step in the edit form.
 */
data class EditableStep(
    val id: String = UUID.randomUUID().toString(),
    val description: String = "",
    val existingImageUrl: String? = null,  // Existing image from R2
    val existingR2Key: String? = null,     // R2 key of existing image
    val newImageUri: Uri? = null,          // New image selected by user
    val newImageR2Key: String? = null,     // R2 key after upload
    val removeExistingImage: Boolean = false  // Flag to remove existing image
)

/**
 * Form data for editing a recipe.
 */
data class EditRecipeFormData(
    val title: String = "",
    val description: String = "",
    val visibility: RecipeVisibility = RecipeVisibility.PUBLIC,
    val images: List<EditableImage> = emptyList(),
    val ingredients: List<IngredientForm> = listOf(IngredientForm()),
    val steps: List<EditableStep> = listOf(EditableStep()),
    val reminder: String = ""
)

/**
 * UI State for Edit Recipe Screen.
 */
sealed interface EditRecipeUiState {
    /**
     * Loading the recipe data.
     */
    data object Loading : EditRecipeUiState
    
    /**
     * Recipe loaded and ready for editing.
     */
    data class Ready(
        val recipeId: Long,
        val currentStep: Int = 0,
        val totalSteps: Int = 6,
        val formData: EditRecipeFormData = EditRecipeFormData(),
        val errors: FormErrors = FormErrors(),
        val isUploading: Boolean = false,
        val uploadProgress: Float = 0f,
        val isSubmitting: Boolean = false,
        val submitSuccess: Boolean = false,
        val errorMessage: String? = null,
        val showDeleteDialog: Boolean = false,
        val isDeleting: Boolean = false,
        val deleteSuccess: Boolean = false
    ) : EditRecipeUiState {
        val stepTitles = listOf(
            "Basic Info",
            "Images",
            "Ingredients",
            "Steps",
            "Reminder",
            "Review"
        )
        
        val canGoNext: Boolean
            get() = when (currentStep) {
                0 -> formData.title.isNotBlank()
                1 -> formData.images.isNotEmpty()
                2 -> formData.ingredients.any { it.name.isNotBlank() && it.quantity.isNotBlank() }
                3 -> formData.steps.any { it.description.isNotBlank() }
                4 -> true
                5 -> !isSubmitting && !isUploading
                else -> false
            }
        
        val canGoPrevious: Boolean
            get() = currentStep > 0 && !isSubmitting && !isUploading
    }
    
    /**
     * Error loading recipe.
     */
    data class Error(val message: String) : EditRecipeUiState
}
