package com.mehchow.letyoucook.util

import com.mehchow.letyoucook.data.repository.PaginatedRecipes
import com.mehchow.letyoucook.data.repository.RecipeResult

class PaginationManager<T>(
    private val pageSize: Int = 20,
    private val fetchPage: suspend (page: Int, size: Int) -> RecipeResult<PaginatedRecipes>
) {
    // Current pagination state
    var currentPage: Int = 0
        private set

    var isLastPage: Boolean = false
        private set

    var totalElements: Long = 0
        private set

    // Accumulated items across all loaded pages
    private val _items = mutableListOf<T>()
    val items: List<T> get() = _items.toList()

    // Load the first page (or reload from scratch). Call on initial load or pull-to-refresh
    @Suppress("UNCHECKED_CAST")
    suspend fun loadFirstPage(
        mapItems: (PaginatedRecipes) -> List<T> = { it.recipes as List<T> }
    ): PaginationResult<T> {
        // Reset state
        currentPage = 0
        isLastPage = false
        totalElements = 0
        _items.clear()

        return loadPageInternal(0, mapItems)
    }

    // Load the next page. Call this for infinite scroll. Return null if already on last page.
    @Suppress("UNCHECKED_CAST")
    suspend fun loadNextPage(
        mapItems: (PaginatedRecipes) -> List<T> = { it.recipes as List<T> }
    ): PaginationResult<T>? {
        if (isLastPage) return null
        return loadPageInternal(currentPage + 1, mapItems)
    }

    private suspend fun loadPageInternal(
        page: Int,
        mapItems: (PaginatedRecipes) -> List<T>
    ): PaginationResult<T> {
        return when (val result = fetchPage(page, pageSize)) {
            is RecipeResult.Success -> {
                val data = result.data
                currentPage = page
                isLastPage = data.isLastPage
                totalElements = data.totalElements

                val newItems = mapItems(data)
                _items.addAll(newItems)

                PaginationResult.Success(
                    items = items, // Return full accumulated list
                    isLastPage = isLastPage,
                    totalElements = totalElements,
                    isEmpty = items.isEmpty()
                )
            }

            is RecipeResult.Error -> {
                PaginationResult.Error(result.message)
            }
        }
    }

    fun canLoadMore(): Boolean = !isLastPage

    fun reset() {
        currentPage = 0
        isLastPage = false
        totalElements = 0
        _items.clear()
    }
}

sealed class PaginationResult<out T> {
    data class Success<T>(
        val items: List<T>,
        val isLastPage: Boolean,
        val totalElements: Long,
        val isEmpty: Boolean
    ): PaginationResult<T>()

    data class Error(val message: String): PaginationResult<Nothing>()
}