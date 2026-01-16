package com.mehchow.letyoucook.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehchow.letyoucook.data.model.RecipeCard
import com.mehchow.letyoucook.data.repository.AuthRepository
import com.mehchow.letyoucook.data.repository.RecipeRepository
import com.mehchow.letyoucook.data.repository.RecipeResult
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

    private var currentPage = 0
    private val pageSize = 20
    private var allRecipes = mutableListOf<RecipeCard>()
    private var isLastPage = false

    init {
        loadRecipes()
    }

    fun loadRecipes() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            currentPage = 0
            allRecipes.clear()

            when (val result = recipeRepository.getPublicRecipes(currentPage, pageSize)) {
                is RecipeResult.Success -> {
                    val paginatedData = result.data
                    allRecipes.addAll(paginatedData.recipes)
                    isLastPage = paginatedData.isLastPage

                    _uiState.value = if (paginatedData.recipes.isEmpty()) {
                        HomeUiState.Empty
                    } else {
                        HomeUiState.Success(
                            recipes = allRecipes.toList(),
                            hasMorePages = !isLastPage
                        )
                    }
                }

                is RecipeResult.Error -> {
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

            currentPage = 0
            allRecipes.clear()

            when (val result = recipeRepository.getPublicRecipes(currentPage, pageSize)) {
                is RecipeResult.Success -> {
                    val paginatedData = result.data
                    allRecipes.addAll(paginatedData.recipes)
                    isLastPage = paginatedData.isLastPage

                    _uiState.value = if (paginatedData.recipes.isEmpty()) {
                        HomeUiState.Empty
                    } else {
                        HomeUiState.Success(
                            recipes = allRecipes.toList(),
                            isRefreshing = false,
                            hasMorePages = !isLastPage
                        )
                    }
                }

                is RecipeResult.Error -> {
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

            currentPage++

            when (val result = recipeRepository.getPublicRecipes(currentPage, pageSize)) {
                is RecipeResult.Success -> {
                    val paginatedData = result.data
                    allRecipes.addAll(paginatedData.recipes)
                    isLastPage = paginatedData.isLastPage

                    _uiState.value = HomeUiState.Success(
                        recipes = allRecipes.toList(),
                        isLoadingMore = false,
                        hasMorePages = !isLastPage
                    )
                }
                is RecipeResult.Error -> {
                    // On pagination error, just stop loading more (don't show error screen)
                    currentPage-- // Revert page increment
                    _uiState.value = currentState.copy(isLoadingMore = false)
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