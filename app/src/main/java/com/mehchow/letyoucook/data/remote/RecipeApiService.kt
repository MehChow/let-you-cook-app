package com.mehchow.letyoucook.data.remote

import com.mehchow.letyoucook.data.model.CreateRecipeRequest
import com.mehchow.letyoucook.data.model.LikedResponse
import com.mehchow.letyoucook.data.model.RecipeCard
import com.mehchow.letyoucook.data.model.RecipeDetail
import com.mehchow.letyoucook.data.model.UpdateRecipeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface RecipeApiService {

    // ==================== PUBLIC RECIPES ====================
    @GET("api/recipes")
    suspend fun getPublicRecipes(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): List<RecipeCard>

    // ==================== MY RECIPES ====================
    @GET("api/recipes/my")
    suspend fun getMyRecipes(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): List<RecipeCard>

    // ==================== USER'S PUBLIC RECIPES ====================
    @GET("api/recipes/user/{userId}")
    suspend fun getUserPublicRecipes(
        @Path("userId") userId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): List<RecipeCard>

    // ==================== SINGLE RECIPE ====================
    @GET("api/recipes/{id}")
    suspend fun getRecipeDetail(
        @Path("id") id: Long,
    ): RecipeDetail

    // ==================== CREATE & UPDATE ====================
    @POST("api/recipes")
    suspend fun createRecipe(
        @Body request: CreateRecipeRequest
    ): RecipeDetail

    @PUT("api/recipes/{id}")
    suspend fun updateRecipe(
        @Path("id") id: Long,
        @Body request: UpdateRecipeRequest
    ): RecipeDetail

    @DELETE("api/recipes/{id}")
    suspend fun deleteRecipe(
        @Path("id") id: Long
    ): Response<Unit>

    // ==================== LIKES ====================
    @POST("api/recipes/{id}/like")
    suspend fun likeRecipe(
        @Path("id") id: Long
    ): Response<Unit>

    @DELETE("api/recipes/{id}/like")
    suspend fun unlikeRecipe(
        @Path("id") id: Long
    ): Response<Unit>

    @GET("api/recipes/{id}/liked")
    suspend fun isRecipeLiked(
        @Path("id") id: Long
    ): LikedResponse
}