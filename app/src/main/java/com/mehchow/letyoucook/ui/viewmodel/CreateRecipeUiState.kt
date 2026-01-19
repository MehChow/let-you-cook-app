package com.mehchow.letyoucook.ui.viewmodel

import android.net.Uri
import com.mehchow.letyoucook.data.model.RecipeVisibility
import java.util.UUID

// Represent a single ingredient in the form
data class IngredientForm(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val quantity: String = "",
    val unit: String = "",
)

// Represent a single step in the form
data class StepForm(
    val id: String = UUID.randomUUID().toString(),
    val description: String = "",
    val imageUri: Uri? = null,
)

// Represent a selected image
data class SelectedImage(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val uploadProgress: Float = 0f,
    val isUploaded: Boolean = false,
    val r2Key: String? = null,
)

// Form data for creating a recipe
data class RecipeFormData(
    val title: String = "",
    val description: String = "",
    val visibility: RecipeVisibility = RecipeVisibility.PUBLIC,
    val images: List<SelectedImage> = emptyList(),
    val ingredients: List<IngredientForm> = listOf(IngredientForm()),
    val steps: List<StepForm> = listOf(StepForm()),
    val reminder: String = "",
)

// Validation errors for each step
data class FormErrors(
    val titleError: String? = null,
    val ingredientError: String? = null,
    val stepsError: String? = null,
)

// UI State for Create Recipe Screen
data class CreateRecipeUiState(
    val currentStep: Int = 0,
    val totalSteps: Int = 6,
    val formData: RecipeFormData = RecipeFormData(),
    val errors: FormErrors = FormErrors(),
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val errorMessage: String? = null,
) {
    val stepTitles = listOf(
        "Basic Info",
        "Images",
        "Ingredients",
        "Steps",
        "Reminder",
        "Review",
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