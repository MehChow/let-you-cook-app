package com.mehchow.letyoucook.ui.viewmodel

import com.mehchow.letyoucook.data.model.RecipeDetail

/**
 * Represents all possible UI states for the Recipe Detail screen.
 */
sealed interface RecipeDetailUiState {
    /**
     * Loading state - fetching recipe details.
     */
    data object Loading : RecipeDetailUiState

    /**
     * Successfully loaded recipe details.
     * @param recipe The full recipe data
     * @param isLikeLoading True when like/unlike action is in progress
     * @param currentUserId The ID of the currently logged-in user (for ownership check)
     * @param showDeleteDialog True when delete confirmation dialog should be shown
     * @param isDeleting True when delete operation is in progress
     */
    data class Success(
        val recipe: RecipeDetail,
        val isLikeLoading: Boolean = false,
        val currentUserId: Long? = null,
        val showDeleteDialog: Boolean = false,
        val isDeleting: Boolean = false
    ) : RecipeDetailUiState {
        val isOwner: Boolean
            get() = currentUserId != null && currentUserId == recipe.creator.id
    }

    /**
     * Error state - failed to load recipe.
     */
    data class Error(val message: String) : RecipeDetailUiState
}
