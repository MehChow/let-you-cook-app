package com.mehchow.letyoucook.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehchow.letyoucook.data.model.RecipeCard
import com.mehchow.letyoucook.data.repository.AuthRepository
import com.mehchow.letyoucook.data.repository.RecipeRepository
import com.mehchow.letyoucook.util.PaginationManager
import com.mehchow.letyoucook.util.PaginationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val paginationManager = PaginationManager<RecipeCard>(
        pageSize = 20,
        fetchPage = { page, size -> recipeRepository.getPublicRecipes(page, size) }
    )

    init {
        loadRecipes()
    }

    fun loadRecipes() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            when (val result = paginationManager.loadFirstPage { it.recipes }) {
                is PaginationResult.Success -> {
                    _uiState.value = if (result.isEmpty) {
                        HomeUiState.Empty
                    } else {
                        HomeUiState.Success(
                            recipes = result.items,
                            hasMorePages = !result.isLastPage
                        )
                    }
                }

                is PaginationResult.Error -> {
                    _uiState.value = HomeUiState.Error(result.message)
                }
            }
        }
    }

    fun refreshRecipes() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is HomeUiState.Success) {
                _uiState.value = currentState.copy(isRefreshing = true)
            }

            when (val result = paginationManager.loadFirstPage { it.recipes }) {
                is PaginationResult.Success -> {
                    _uiState.value = if (result.isEmpty) {
                        HomeUiState.Empty
                    } else {
                        HomeUiState.Success(
                            recipes = result.items,
                            isRefreshing = false,
                            hasMorePages = !result.isLastPage
                        )
                    }
                }

                is PaginationResult.Error -> {
                    _uiState.value = HomeUiState.Error(result.message)
                }
            }
        }
    }

    fun loadMoreRecipes() {
        val currentState = _uiState.value
        // Only load more if we're in Success state and not already loading
        if (currentState !is HomeUiState.Success) return
        if (currentState.isLoadingMore || !currentState.hasMorePages) return

        viewModelScope.launch {
            // Show loading more indicator
            _uiState.value = currentState.copy(isLoadingMore = true)

            when (val result = paginationManager.loadNextPage { it.recipes }) {
                is PaginationResult.Success -> {
                    _uiState.value = HomeUiState.Success(
                        recipes = result.items,
                        isLoadingMore = false,
                        hasMorePages = !result.isLastPage
                    )
                }
                is PaginationResult.Error -> {
                    // On pagination error, just stop loading more (don't show error screen)
                    _uiState.value = currentState.copy(isLoadingMore = false)
                }
                null -> {
                    // No more pages
                    _uiState.value = currentState.copy(isLoadingMore = false, hasMorePages = false)
                }
            }
        }
    }

    fun onLogoutClick() {
        viewModelScope.launch {
            authRepository.logout()
            // The AppNavigation will automatically react to the cleared DataStore
        }
    }
}