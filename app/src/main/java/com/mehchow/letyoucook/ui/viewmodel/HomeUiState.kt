package com.mehchow.letyoucook.ui.viewmodel

import com.mehchow.letyoucook.data.model.RecipeCard

sealed interface HomeUiState {

    data object Loading: HomeUiState

    data class Success(
        val recipes: List<RecipeCard>,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = true,
    ): HomeUiState

    data class Error(val message: String): HomeUiState

    data object Empty: HomeUiState
}