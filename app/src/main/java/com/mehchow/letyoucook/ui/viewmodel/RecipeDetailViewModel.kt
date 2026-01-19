package com.mehchow.letyoucook.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehchow.letyoucook.data.repository.RecipeRepository
import com.mehchow.letyoucook.data.repository.RecipeResult
import com.mehchow.letyoucook.data.repository.UserRepository
import com.mehchow.letyoucook.data.repository.UserResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Recipe Detail screen.
 *
 * @param savedStateHandle Provides access to navigation arguments (recipeId)
 * @param recipeRepository Repository for recipe operations
 * @param userRepository Repository for user operations
 */
@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // Get recipeId from navigation arguments
    private val recipeId: Long = savedStateHandle.get<Long>("recipeId") ?: 0L
    
    // Cache current user ID
    private var currentUserId: Long? = null

    private val _uiState = MutableStateFlow<RecipeDetailUiState>(RecipeDetailUiState.Loading)
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()
    
    // Event for navigation after delete
    private val _deleteSuccess = MutableStateFlow(false)
    val deleteSuccess: StateFlow<Boolean> = _deleteSuccess.asStateFlow()

    init {
        loadRecipeDetail()
    }

    /**
     * Load recipe details from API.
     */
    fun loadRecipeDetail() {
        viewModelScope.launch {
            _uiState.value = RecipeDetailUiState.Loading
            
            // Fetch current user if not cached
            if (currentUserId == null) {
                when (val userResult = userRepository.getCurrentUserProfile()) {
                    is UserResult.Success -> currentUserId = userResult.data.id
                    is UserResult.Error -> { /* Continue without user ID */ }
                }
            }

            when (val result = recipeRepository.getRecipeDetail(recipeId)) {
                is RecipeResult.Success -> {
                    _uiState.value = RecipeDetailUiState.Success(
                        recipe = result.data,
                        currentUserId = currentUserId
                    )
                }
                is RecipeResult.Error -> {
                    _uiState.value = RecipeDetailUiState.Error(result.message)
                }
            }
        }
    }

    /**
     * Toggle like status for this recipe.
     */
    fun toggleLike() {
        val currentState = _uiState.value
        if (currentState !is RecipeDetailUiState.Success) return
        if (currentState.isLikeLoading) return

        val recipe = currentState.recipe
        val isCurrentlyLiked = recipe.isLikedByCurrentUser

        viewModelScope.launch {
            // Show loading state
            _uiState.value = currentState.copy(isLikeLoading = true)

            val result = if (isCurrentlyLiked) {
                recipeRepository.unlikeRecipe(recipeId)
            } else {
                recipeRepository.likeRecipe(recipeId)
            }

            when (result) {
                is RecipeResult.Success -> {
                    // Update local state optimistically
                    val newLikeCount = if (isCurrentlyLiked) {
                        recipe.likeCount - 1
                    } else {
                        recipe.likeCount + 1
                    }

                    val updatedRecipe = recipe.copy(
                        isLikedByCurrentUser = !isCurrentlyLiked,
                        likeCount = newLikeCount.coerceAtLeast(0)
                    )

                    _uiState.value = RecipeDetailUiState.Success(
                        recipe = updatedRecipe,
                        isLikeLoading = false
                    )
                }
                is RecipeResult.Error -> {
                    // Revert to previous state on error
                    _uiState.value = currentState.copy(isLikeLoading = false)
                }
            }
        }
    }
    
    /**
     * Show delete confirmation dialog.
     */
    fun showDeleteDialog() {
        val currentState = _uiState.value
        if (currentState is RecipeDetailUiState.Success) {
            _uiState.value = currentState.copy(showDeleteDialog = true)
        }
    }
    
    /**
     * Hide delete confirmation dialog.
     */
    fun hideDeleteDialog() {
        val currentState = _uiState.value
        if (currentState is RecipeDetailUiState.Success) {
            _uiState.value = currentState.copy(showDeleteDialog = false)
        }
    }
    
    /**
     * Delete the recipe.
     */
    fun deleteRecipe() {
        val currentState = _uiState.value
        if (currentState !is RecipeDetailUiState.Success) return
        if (currentState.isDeleting) return
        
        viewModelScope.launch {
            _uiState.value = currentState.copy(isDeleting = true, showDeleteDialog = false)
            
            when (val result = recipeRepository.deleteRecipe(recipeId)) {
                is RecipeResult.Success -> {
                    _deleteSuccess.value = true
                }
                is RecipeResult.Error -> {
                    _uiState.value = currentState.copy(
                        isDeleting = false,
                        showDeleteDialog = false
                    )
                    // Could add error handling here
                }
            }
        }
    }
    
    /**
     * Reset delete success flag after navigation.
     */
    fun resetDeleteSuccess() {
        _deleteSuccess.value = false
    }
}
