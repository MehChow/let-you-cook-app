package com.mehchow.letyoucook.ui.viewmodel

import com.mehchow.letyoucook.data.model.RecipeCard
import com.mehchow.letyoucook.data.model.UserProfile

sealed interface ProfileUiState {
    data object Loading: ProfileUiState

    data class Success(
        val profile: UserProfile,
        val recipes: List<RecipeCard>,
        val totalRecipes: Long,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = true,
    ): ProfileUiState

    data class Error(val message: String): ProfileUiState
}