package com.mehchow.letyoucook.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehchow.letyoucook.data.model.RecipeCard
import com.mehchow.letyoucook.data.model.UserProfile
import com.mehchow.letyoucook.data.repository.AuthRepository
import com.mehchow.letyoucook.data.repository.RecipeRepository
import com.mehchow.letyoucook.data.repository.UserRepository
import com.mehchow.letyoucook.data.repository.UserResult
import com.mehchow.letyoucook.util.PaginationManager
import com.mehchow.letyoucook.util.PaginationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val recipeRepository: RecipeRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val paginationManager = PaginationManager<RecipeCard>(
        pageSize = 20,
        fetchPage = { page, size -> recipeRepository.getMyRecipes(page, size) }
    )

    // Cache the profile so we don't refetch on pagination
    private var cachedProfile: UserProfile? = null

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading

            when (val profileResult = userRepository.getCurrentUserProfile()) {
                is UserResult.Success -> {
                    cachedProfile = profileResult.data
                    loadRecipesForProfile(profileResult.data)
                }

                is UserResult.Error -> {
                    _uiState.value = ProfileUiState.Error(profileResult.message)
                }
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is ProfileUiState.Success) {
                _uiState.value = currentState.copy(isRefreshing = true)
            }

            when (val profileResult = userRepository.getCurrentUserProfile()) {
                is UserResult.Success -> {
                    cachedProfile = profileResult.data

                    when (val recipesResult = paginationManager.loadFirstPage { it.recipes }) {
                        is PaginationResult.Success -> {
                            _uiState.value = ProfileUiState.Success(
                                profile = profileResult.data,
                                recipes = recipesResult.items,
                                totalRecipes = recipesResult.totalElements,
                                isRefreshing = false,
                                hasMorePages = !recipesResult.isLastPage
                            )
                        }

                        is PaginationResult.Error -> {
                            _uiState.value = ProfileUiState.Error(recipesResult.message)
                        }
                    }
                }

                is UserResult.Error -> {
                    _uiState.value = ProfileUiState.Error(profileResult.message)
                }
            }
        }
    }

    fun loadMoreRecipes() {
        val currentState = _uiState.value
        // Only load more if we're in Success state and not already loading
        if (currentState !is ProfileUiState.Success) return
        if (currentState.isLoadingMore || !currentState.hasMorePages) return

        viewModelScope.launch {
            // Show loading more indicator
            _uiState.value = currentState.copy(isLoadingMore = true)

            when (val result = paginationManager.loadNextPage { it.recipes }) {
                is PaginationResult.Success -> {
                    _uiState.value = currentState.copy(
                        recipes = result.items,
                        isLoadingMore = false,
                        hasMorePages = !result.isLastPage
                    )
                }

                is PaginationResult.Error -> {
                    _uiState.value = currentState.copy(isLoadingMore = false)
                }

                null -> {
                    _uiState.value = currentState.copy(isLoadingMore = false, hasMorePages = false)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    private suspend fun loadRecipesForProfile(profile: UserProfile) {
        when (val result = paginationManager.loadFirstPage { it.recipes }) {
            is PaginationResult.Success -> {
                _uiState.value = ProfileUiState.Success(
                    profile = profile,
                    recipes = result.items,
                    totalRecipes = result.totalElements,
                    hasMorePages = !result.isLastPage
                )
            }

            is PaginationResult.Error -> {
                _uiState.value = ProfileUiState.Error(result.message)
            }
        }
    }
}