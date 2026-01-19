package com.mehchow.letyoucook.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehchow.letyoucook.data.model.CreateImageRequest
import com.mehchow.letyoucook.data.model.CreateIngredientRequest
import com.mehchow.letyoucook.data.model.CreateStepRequest
import com.mehchow.letyoucook.data.model.RecipeVisibility
import com.mehchow.letyoucook.data.model.UpdateRecipeRequest
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
class EditRecipeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val uploadRepository: UploadRepository
) : ViewModel() {

    private val recipeId: Long = savedStateHandle.get<Long>("recipeId") ?: 0L
    
    private val _uiState = MutableStateFlow<EditRecipeUiState>(EditRecipeUiState.Loading)
    val uiState: StateFlow<EditRecipeUiState> = _uiState.asStateFlow()

    init {
        loadRecipe()
    }

    /**
     * Load existing recipe data for editing.
     */
    private fun loadRecipe() {
        viewModelScope.launch {
            _uiState.value = EditRecipeUiState.Loading
            
            when (val result = recipeRepository.getRecipeDetail(recipeId)) {
                is RecipeResult.Success -> {
                    val recipe = result.data
                    
                    // Convert recipe to form data
                    val formData = EditRecipeFormData(
                        title = recipe.title,
                        description = recipe.description ?: "",
                        visibility = recipe.visibility,
                        images = recipe.images.sortedBy { it.displayOrder }.map { image ->
                            // Extract r2Key from imageUrl
                            val r2Key = extractR2KeyFromUrl(image.imageUrl)
                            EditableImage.Existing(
                                id = image.id.toString(),
                                imageUrl = image.imageUrl,
                                r2Key = r2Key,
                                displayOrder = image.displayOrder
                            )
                        },
                        ingredients = recipe.ingredients.map { ingredient ->
                            IngredientForm(
                                id = ingredient.id.toString(),
                                name = ingredient.name,
                                quantity = ingredient.quantity,
                                unit = ingredient.unit ?: ""
                            )
                        }.ifEmpty { listOf(IngredientForm()) },
                        steps = recipe.steps.sortedBy { it.stepNumber }.map { step ->
                            val r2Key = step.imageUrl?.let { extractR2KeyFromUrl(it) }
                            EditableStep(
                                id = step.id.toString(),
                                description = step.description,
                                existingImageUrl = step.imageUrl,
                                existingR2Key = r2Key
                            )
                        }.ifEmpty { listOf(EditableStep()) },
                        reminder = recipe.reminder ?: ""
                    )
                    
                    _uiState.value = EditRecipeUiState.Ready(
                        recipeId = recipeId,
                        formData = formData
                    )
                }
                is RecipeResult.Error -> {
                    _uiState.value = EditRecipeUiState.Error(result.message)
                }
            }
        }
    }
    
    /**
     * Extract R2 key from the full image URL.
     * E.g., "https://bucket.r2.dev/recipes/abc123.jpg" -> "recipes/abc123.jpg"
     */
    private fun extractR2KeyFromUrl(url: String): String {
        // Find the path after the domain
        return try {
            val uri = Uri.parse(url)
            uri.path?.trimStart('/') ?: url
        } catch (e: Exception) {
            url
        }
    }

    // ======================== NAVIGATION ========================
    
    fun nextStep() {
        updateReadyState { state ->
            if (state.currentStep < state.totalSteps - 1 && state.canGoNext) {
                state.copy(currentStep = state.currentStep + 1, errorMessage = null)
            } else state
        }
    }

    fun previousStep() {
        updateReadyState { state ->
            if (state.canGoPrevious) {
                state.copy(currentStep = state.currentStep - 1, errorMessage = null)
            } else state
        }
    }

    fun goToStep(step: Int) {
        updateReadyState { state ->
            if (step in 0 until state.totalSteps && !state.isSubmitting && !state.isUploading) {
                state.copy(currentStep = step, errorMessage = null)
            } else state
        }
    }

    // ==================== STEP 1: BASIC INFO ====================
    
    fun updateTitle(title: String) {
        updateReadyState { state ->
            state.copy(
                formData = state.formData.copy(title = title),
                errors = state.errors.copy(titleError = null)
            )
        }
    }

    fun updateDescription(description: String) {
        updateReadyState { state ->
            state.copy(formData = state.formData.copy(description = description))
        }
    }

    fun updateVisibility(visibility: RecipeVisibility) {
        updateReadyState { state ->
            state.copy(formData = state.formData.copy(visibility = visibility))
        }
    }

    // ==================== STEP 2: IMAGES ====================
    
    fun addImages(uris: List<Uri>) {
        updateReadyState { state ->
            val currentImages = state.formData.images
            val remainingSlots = 10 - currentImages.size
            val nextDisplayOrder = currentImages.maxOfOrNull { it.displayOrder }?.plus(1) ?: 0
            
            val newImages = uris.take(remainingSlots).mapIndexed { index, uri ->
                EditableImage.New(
                    uri = uri,
                    displayOrder = nextDisplayOrder + index
                )
            }
            state.copy(
                formData = state.formData.copy(images = currentImages + newImages)
            )
        }
    }

    fun removeImage(imageId: String) {
        updateReadyState { state ->
            state.copy(
                formData = state.formData.copy(
                    images = state.formData.images.filter { it.id != imageId }
                )
            )
        }
    }

    // ==================== STEP 3: INGREDIENTS ====================
    
    fun addIngredient() {
        updateReadyState { state ->
            state.copy(
                formData = state.formData.copy(
                    ingredients = state.formData.ingredients + IngredientForm()
                )
            )
        }
    }

    fun removeIngredient(ingredientId: String) {
        updateReadyState { state ->
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
        updateReadyState { state ->
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
        updateReadyState { state ->
            state.copy(
                formData = state.formData.copy(
                    steps = state.formData.steps + EditableStep()
                )
            )
        }
    }

    fun removeStep(stepId: String) {
        updateReadyState { state ->
            val steps = state.formData.steps.filter { it.id != stepId }
            state.copy(
                formData = state.formData.copy(
                    steps = steps.ifEmpty { listOf(EditableStep()) }
                )
            )
        }
    }

    fun updateStepDescription(stepId: String, description: String) {
        updateReadyState { state ->
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
        updateReadyState { state ->
            state.copy(
                formData = state.formData.copy(
                    steps = state.formData.steps.map { step ->
                        if (step.id == stepId) {
                            step.copy(
                                newImageUri = imageUri,
                                removeExistingImage = imageUri == null && step.existingImageUrl != null
                            )
                        } else step
                    }
                )
            )
        }
    }
    
    fun removeStepImage(stepId: String) {
        updateReadyState { state ->
            state.copy(
                formData = state.formData.copy(
                    steps = state.formData.steps.map { step ->
                        if (step.id == stepId) {
                            step.copy(
                                newImageUri = null,
                                removeExistingImage = true
                            )
                        } else step
                    }
                )
            )
        }
    }

    // ==================== STEP 5: REMINDER ====================
    
    fun updateReminder(reminder: String) {
        updateReadyState { state ->
            state.copy(formData = state.formData.copy(reminder = reminder))
        }
    }

    // ==================== STEP 6: SUBMIT ====================
    
    fun submitRecipe() {
        val currentState = _uiState.value
        if (currentState !is EditRecipeUiState.Ready) return
        
        viewModelScope.launch {
            val formData = currentState.formData

            // Validate
            if (formData.title.isBlank()) {
                _uiState.value = currentState.copy(errorMessage = "Title is required")
                return@launch
            }

            if (formData.images.isEmpty()) {
                _uiState.value = currentState.copy(errorMessage = "At least one image is required")
                return@launch
            }

            if (formData.ingredients.none { it.name.isNotBlank() && it.quantity.isNotBlank() }) {
                _uiState.value = currentState.copy(errorMessage = "At least one ingredient with name and quantity is required")
                return@launch
            }

            if (formData.steps.none { it.description.isNotBlank() }) {
                _uiState.value = currentState.copy(errorMessage = "At least one step is required")
                return@launch
            }

            _uiState.value = currentState.copy(isUploading = true, errorMessage = null)

            try {
                // Collect all new images that need uploading
                val uploadedImages = mutableMapOf<String, String>()
                val allImagesToUpload = mutableListOf<Pair<String, Uri>>()

                // New cover images
                formData.images.filterIsInstance<EditableImage.New>().forEach { image ->
                    allImagesToUpload.add(image.id to image.uri)
                }

                // New step images
                formData.steps.forEach { step ->
                    step.newImageUri?.let { uri ->
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
                            _uiState.value = currentState.copy(
                                isUploading = false,
                                errorMessage = "Failed to upload images: ${uploadResult.message}"
                            )
                            return@launch
                        }
                    }
                }

                _uiState.value = (_uiState.value as EditRecipeUiState.Ready).copy(
                    isUploading = false, 
                    isSubmitting = true
                )

                // Build update request
                val imageRequests = formData.images.mapIndexedNotNull { index, image ->
                    when (image) {
                        is EditableImage.Existing -> CreateImageRequest(
                            r2Key = image.r2Key,
                            displayOrder = index
                        )
                        is EditableImage.New -> uploadedImages[image.id]?.let { r2Key ->
                            CreateImageRequest(
                                r2Key = r2Key,
                                displayOrder = index
                            )
                        }
                    }
                }

                val request = UpdateRecipeRequest(
                    title = formData.title,
                    description = formData.description.ifBlank { null },
                    visibility = formData.visibility,
                    images = imageRequests,
                    ingredients = formData.ingredients
                        .filter { it.name.isNotBlank() && it.quantity.isNotBlank() }
                        .map { ingredient ->
                            CreateIngredientRequest(
                                name = ingredient.name,
                                quantity = ingredient.quantity,
                                unit = ingredient.unit.ifBlank { null }
                            )
                        },
                    steps = formData.steps
                        .filter { it.description.isNotBlank() }
                        .mapIndexed { index, step ->
                            // Determine which r2Key to use
                            val r2Key = when {
                                // New image was uploaded
                                uploadedImages.containsKey(step.id) -> uploadedImages[step.id]
                                // Existing image should be removed
                                step.removeExistingImage -> null
                                // Keep existing image
                                else -> step.existingR2Key
                            }
                            CreateStepRequest(
                                stepNumber = index + 1,
                                description = step.description,
                                r2Key = r2Key
                            )
                        },
                    reminder = formData.reminder.ifBlank { null }
                )

                // Submit to API
                when (val result = recipeRepository.updateRecipe(recipeId, request)) {
                    is RecipeResult.Success -> {
                        _uiState.value = (_uiState.value as EditRecipeUiState.Ready).copy(
                            isSubmitting = false, 
                            submitSuccess = true
                        )
                    }
                    is RecipeResult.Error -> {
                        _uiState.value = (_uiState.value as EditRecipeUiState.Ready).copy(
                            isSubmitting = false,
                            errorMessage = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = (_uiState.value as? EditRecipeUiState.Ready)?.copy(
                    isUploading = false,
                    isSubmitting = false,
                    errorMessage = "Unexpected error: ${e.localizedMessage}"
                ) ?: currentState
            }
        }
    }
    
    // ==================== DELETE ====================
    
    fun showDeleteDialog() {
        updateReadyState { state -> state.copy(showDeleteDialog = true) }
    }
    
    fun hideDeleteDialog() {
        updateReadyState { state -> state.copy(showDeleteDialog = false) }
    }
    
    fun deleteRecipe() {
        val currentState = _uiState.value
        if (currentState !is EditRecipeUiState.Ready) return
        if (currentState.isDeleting) return
        
        viewModelScope.launch {
            _uiState.value = currentState.copy(isDeleting = true, showDeleteDialog = false)
            
            when (val result = recipeRepository.deleteRecipe(recipeId)) {
                is RecipeResult.Success -> {
                    _uiState.value = (_uiState.value as EditRecipeUiState.Ready).copy(
                        isDeleting = false,
                        deleteSuccess = true
                    )
                }
                is RecipeResult.Error -> {
                    _uiState.value = (_uiState.value as EditRecipeUiState.Ready).copy(
                        isDeleting = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun clearError() {
        updateReadyState { state -> state.copy(errorMessage = null) }
    }
    
    /**
     * Helper to update Ready state safely.
     */
    private inline fun updateReadyState(transform: (EditRecipeUiState.Ready) -> EditRecipeUiState.Ready) {
        _uiState.update { currentState ->
            if (currentState is EditRecipeUiState.Ready) {
                transform(currentState)
            } else {
                currentState
            }
        }
    }
}
