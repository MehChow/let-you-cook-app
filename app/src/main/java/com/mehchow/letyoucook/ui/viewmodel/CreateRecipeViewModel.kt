package com.mehchow.letyoucook.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehchow.letyoucook.data.model.CreateImageRequest
import com.mehchow.letyoucook.data.model.CreateIngredientRequest
import com.mehchow.letyoucook.data.model.CreateRecipeRequest
import com.mehchow.letyoucook.data.model.CreateStepRequest
import com.mehchow.letyoucook.data.model.RecipeVisibility
import com.mehchow.letyoucook.data.repository.RecipeRepository
import com.mehchow.letyoucook.data.repository.RecipeResult
import com.mehchow.letyoucook.data.repository.UploadRepository
import com.mehchow.letyoucook.data.repository.UploadResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateRecipeViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val uploadRepository: UploadRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateRecipeUiState())
    val uiState: StateFlow<CreateRecipeUiState> = _uiState.asStateFlow()

    // ======================== NAVIGATION ========================
    fun nextStep() {
        _uiState.update { state ->
            if (state.currentStep < state.totalSteps - 1 && state.canGoNext) {
                state.copy(currentStep = state.currentStep + 1, errorMessage = null)
            } else state
        }
    }

    fun previousStep() {
        _uiState.update { state ->
            if (state.canGoPrevious) {
                state.copy(currentStep = state.currentStep - 1, errorMessage = null)
            } else state
        }
    }

    fun goToStep(step: Int) {
        _uiState.update { state ->
            if (step in 0 until state.totalSteps && !state.isSubmitting && !state.isUploading) {
                state.copy(currentStep = step, errorMessage = null)
            } else state
        }
    }

    // ==================== STEP 1: BASIC INFO ====================
    fun updateTitle(title: String) {
        _uiState.update { state ->
            state.copy(
                formData = state.formData.copy(title = title),
                errors = state.errors.copy(titleError = null)
            )
        }
    }

    fun updateDescription(description: String) {
        _uiState.update { state ->
            state.copy(formData = state.formData.copy(description = description))
        }
    }

    fun updateVisibility(visibility: RecipeVisibility) {
        _uiState.update { state ->
            state.copy(formData = state.formData.copy(visibility = visibility))
        }
    }

    // ==================== STEP 2: IMAGES ====================
    fun addImages(uris: List<Uri>) {
        _uiState.update { state ->
            val currentImages = state.formData.images
            val remainingSlots = 10 - currentImages.size
            val newImages = uris.take(remainingSlots).map { uri ->
                SelectedImage(uri = uri)
            }
            state.copy(
                formData = state.formData.copy(images = currentImages + newImages)
            )
        }
    }

    fun removeImage(imageId: String) {
        _uiState.update { state ->
            state.copy(
                formData = state.formData.copy(
                    images = state.formData.images.filter { it.id != imageId }
                )
            )
        }
    }

    // ==================== STEP 3: INGREDIENTS ====================
    fun addIngredient() {
        _uiState.update { state ->
            state.copy(
                formData = state.formData.copy(
                    ingredients = state.formData.ingredients + IngredientForm()
                )
            )
        }
    }

    fun removeIngredient(ingredientId: String) {
        _uiState.update { state ->
            val ingredients = state.formData.ingredients.filter { it.id != ingredientId }
            state.copy(
                formData = state.formData.copy(
                    ingredients = ingredients.ifEmpty { listOf(IngredientForm()) }
                )
            )
        }
    }

    fun updateIngredient(
        ingredientId: String,
        name: String? = null,
        quantity: String? = null,
        unit: String? = null
    ) {
        _uiState.update { state ->
            state.copy(
                formData = state.formData.copy(
                    ingredients = state.formData.ingredients.map { ingredient ->
                        if (ingredient.id == ingredientId) {
                            ingredient.copy(
                                name = name ?: ingredient.name,
                                quantity = quantity ?: ingredient.quantity,
                                unit = unit ?: ingredient.unit
                            )
                        } else ingredient
                    }
                )
            )
        }
    }

    // ==================== STEP 4: STEPS ====================
    fun addStep() {
        _uiState.update { state ->
            state.copy(
                formData = state.formData.copy(
                    steps = state.formData.steps + StepForm()
                )
            )
        }
    }

    fun removeStep(stepId: String) {
        _uiState.update { state ->
            val steps = state.formData.steps.filter { it.id != stepId }
            state.copy(
                formData = state.formData.copy(
                    steps = steps.ifEmpty { listOf(StepForm()) }
                )
            )
        }
    }

    fun updateStepDescription(stepId: String, description: String) {
        _uiState.update { state ->
            state.copy(
                formData = state.formData.copy(
                    steps = state.formData.steps.map { step ->
                        if (step.id == stepId) step.copy(description = description) else step
                    }
                )
            )
        }
    }

    fun updateStepImage(stepId: String, imageUri: Uri?) {
        _uiState.update { state ->
            state.copy(
                formData = state.formData.copy(
                    steps = state.formData.steps.map { step ->
                        if (step.id == stepId) step.copy(imageUri = imageUri) else step
                    }
                )
            )
        }
    }

    // ==================== STEP 5: REMINDER ====================
    fun updateReminder(reminder: String) {
        _uiState.update { state ->
            state.copy(formData = state.formData.copy(reminder = reminder))
        }
    }

    // ==================== STEP 6: SUBMIT ====================
    fun submitRecipe() {
        viewModelScope.launch {
            val formData = _uiState.value.formData

            // Validate
            if (formData.title.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Title is required") }
                return@launch
            }

            if (formData.images.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "At least one image is required") }
                return@launch
            }

            if (formData.ingredients.none { it.name.isNotBlank() && it.quantity.isNotBlank() }) {
                _uiState.update { it.copy(errorMessage = "At least one ingredient with name and quantity is required") }
                return@launch
            }

            if (formData.steps.none { it.description.isNotBlank() }) {
                _uiState.update { it.copy(errorMessage = "At least one step is required") }
                return@launch
            }

            _uiState.update { it.copy(isUploading = true, errorMessage = null) }

            try {
                // Upload all images
                val uploadedImages = mutableMapOf<String, String>()
                val allImagesToUpload = mutableListOf<Pair<String, Uri>>()

                // Cover images
                formData.images.forEach { image ->
                    allImagesToUpload.add(image.id to image.uri)
                }

                // Step images
                formData.steps.forEach { step ->
                    step.imageUri?.let { uri ->
                        allImagesToUpload.add(step.id to uri)
                    }
                }

                if (allImagesToUpload.isNotEmpty()) {
                    val urisToUpload = allImagesToUpload.map { it.second }

                    when (val uploadResult = uploadRepository.uploadImages(urisToUpload)) {
                        is UploadResult.Success -> {
                            uploadResult.data.forEachIndexed { index, uploadedImage ->
                                val (id, _) = allImagesToUpload[index]
                                uploadedImages[id] = uploadedImage.r2Key
                            }
                        }

                        is UploadResult.Error -> {
                            _uiState.update {
                                it.copy(
                                    isUploading = false,
                                    errorMessage = "Failed to upload images: ${uploadResult.message}"
                                )
                            }
                            return@launch
                        }
                    }
                }

                _uiState.update { it.copy(isUploading = false, isSubmitting = true) }

                // Create recipe request
                val request = CreateRecipeRequest(
                    title = formData.title,
                    description = formData.description.ifBlank { null },
                    visibility = formData.visibility,
                    images = formData.images.mapIndexedNotNull { index, image ->
                        uploadedImages[image.id]?.let { r2Key ->
                            CreateImageRequest(
                                r2Key = r2Key,
                                displayOrder = index
                            )
                        }
                    },
                    ingredients = formData.ingredients
                        .filter { it.name.isNotBlank() && it.quantity.isNotBlank() }
                        .mapIndexed { index, ingredient ->
                            CreateIngredientRequest(
                                name = ingredient.name,
                                quantity = ingredient.quantity,
                                unit = ingredient.unit.ifBlank { null }
                            )
                        },
                    steps = formData.steps
                        .filter { it.description.isNotBlank() }
                        .mapIndexed { index, step ->
                            CreateStepRequest(
                                stepNumber = index + 1,
                                description = step.description,
                                r2Key = uploadedImages[step.id]
                            )
                        },
                    reminder = formData.reminder.ifBlank { null }
                )

                // Submit to API
                when (val result = recipeRepository.createRecipe(request)) {
                    is RecipeResult.Success -> {
                        _uiState.update { it.copy(isSubmitting = false, submitSuccess = true) }
                    }

                    is RecipeResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = result.message
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        isSubmitting = false,
                        errorMessage = "Unexpected error: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetForm() {
        _uiState.value = CreateRecipeUiState()
    }
}