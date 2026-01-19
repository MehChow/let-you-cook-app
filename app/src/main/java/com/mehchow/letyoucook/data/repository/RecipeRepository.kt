package com.mehchow.letyoucook.data.repository

import com.mehchow.letyoucook.data.model.CreateRecipeRequest
import com.mehchow.letyoucook.data.model.PageResponse
import com.mehchow.letyoucook.data.model.RecipeCard
import com.mehchow.letyoucook.data.model.RecipeDetail
import com.mehchow.letyoucook.data.model.UpdateRecipeRequest
import com.mehchow.letyoucook.data.remote.RecipeApiService
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

sealed class RecipeResult<out T> {
    data class Success<T>(val data: T) : RecipeResult<T>()
    data class Error(val message: String, val code: Int? = null): RecipeResult<Nothing>()
}

data class PaginatedRecipes(
    val recipes: List<RecipeCard>,
    val isLastPage: Boolean,
    val totalPages: Int,
    val currentPage: Int,
    val totalElements: Long,
)

interface RecipeRepository {
    suspend fun getPublicRecipes(page: Int, size: Int): RecipeResult<PaginatedRecipes>
    suspend fun getMyRecipes(page: Int, size: Int): RecipeResult<PaginatedRecipes>
    suspend fun getUserPublicRecipes(userId: Long, page: Int, size: Int): RecipeResult<PaginatedRecipes>
    suspend fun getRecipeDetail(id: Long): RecipeResult<RecipeDetail>
    suspend fun createRecipe(request: CreateRecipeRequest): RecipeResult<RecipeDetail>
    suspend fun updateRecipe(id: Long, request: UpdateRecipeRequest): RecipeResult<RecipeDetail>
    suspend fun deleteRecipe(id: Long): RecipeResult<Unit>
    suspend fun likeRecipe(id: Long): RecipeResult<Unit>
    suspend fun unlikeRecipe(id: Long): RecipeResult<Unit>
    suspend fun isRecipeLiked(id: Long): RecipeResult<Boolean>
}

@Singleton
class RecipeRepositoryImpl @Inject constructor(
    private val recipeApiService: RecipeApiService
) : RecipeRepository {
    override suspend fun getPublicRecipes(page: Int, size: Int): RecipeResult<PaginatedRecipes> {
        return safeApiCall {
            val response = recipeApiService.getPublicRecipes(page, size)
            response.toPaginatedRecipes()
        }
    }

    override suspend fun getMyRecipes(page: Int, size: Int): RecipeResult<PaginatedRecipes> {
        return safeApiCall {
            val response = recipeApiService.getMyRecipes(page, size)
            response.toPaginatedRecipes()
        }
    }

    override suspend fun getUserPublicRecipes(
        userId: Long,
        page: Int,
        size: Int
    ): RecipeResult<PaginatedRecipes> {
        return safeApiCall {
            val response = recipeApiService.getUserPublicRecipes(userId, page, size)
            response.toPaginatedRecipes()
        }
    }

    override suspend fun getRecipeDetail(id: Long): RecipeResult<RecipeDetail> {
        return safeApiCall {
            recipeApiService.getRecipeDetail(id)
        }
    }

    override suspend fun createRecipe(request: CreateRecipeRequest): RecipeResult<RecipeDetail> {
        return safeApiCall {
            recipeApiService.createRecipe(request)
        }
    }

    override suspend fun updateRecipe(
        id: Long,
        request: UpdateRecipeRequest
    ): RecipeResult<RecipeDetail> {
        return safeApiCall {
            recipeApiService.updateRecipe(id, request)
        }
    }

    override suspend fun deleteRecipe(id: Long): RecipeResult<Unit> {
        return safeApiCall {
            val response = recipeApiService.deleteRecipe(id)
            if (response.isSuccessful) {
                Unit
}           else {
                throw HttpException(response)}
        }
    }

    override suspend fun likeRecipe(id: Long): RecipeResult<Unit> {
        return safeApiCall {
            val response = recipeApiService.likeRecipe(id)
            if (response.isSuccessful) {
                Unit
            } else {
                throw HttpException(response)
            }
        }
    }

    override suspend fun unlikeRecipe(id: Long): RecipeResult<Unit> {
        return safeApiCall {
            val response = recipeApiService.unlikeRecipe(id)
            if (response.isSuccessful) {
                Unit
            } else {
                throw HttpException(response)
            }
        }
    }

    override suspend fun isRecipeLiked(id: Long): RecipeResult<Boolean> {
        return safeApiCall {
            recipeApiService.isRecipeLiked(id).liked
        }
    }

    /**
     * Convert PageResponse (VIA_DTO format) to PaginatedRecipes.
     * Pagination metadata is now nested inside the 'page' object.
     */
    private fun PageResponse<RecipeCard>.toPaginatedRecipes(): PaginatedRecipes {
        return PaginatedRecipes(
            recipes = this.content,
            isLastPage = this.page.isLastPage,
            totalPages = this.page.totalPages,
            currentPage = this.page.number,
            totalElements = this.page.totalElements,
        )
    }

    private suspend fun <T> safeApiCall(block: suspend () -> T): RecipeResult<T> {
        return try {
            RecipeResult.Success(block())
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Session expired. Please log in again."
                403 -> "You do not have permission to perform this action."
                404 -> "Recipe not found."
                409 -> "Action already performed."
                429 -> "Too many requests. Please try again later."
                else -> "Server error: ${e.message()}"
            }
            RecipeResult.Error(errorMessage, e.code())
        } catch (e: UnknownHostException) {
            RecipeResult.Error("No internet connection. Please check your network.")
        } catch (e: SocketTimeoutException) {
            RecipeResult.Error("Request timed out. Please try again.")
        } catch (e: Exception) {
            RecipeResult.Error("An unexpected error occurred: ${e.localizedMessage}")
        }
    }
}