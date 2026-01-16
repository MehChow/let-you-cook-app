package com.mehchow.letyoucook.data.model

import com.google.gson.annotations.SerializedName

// For displaying recipe cards in home screen grid
data class RecipeCard(
    @SerializedName("id")
    val id: Long,

    @SerializedName("title")
    val title: String,

    @SerializedName("coverImageUrl")
    val coverImageUrl: String?,

    @SerializedName("likeCount")
    val likeCount: Int,

    @SerializedName("creatorId")
    val creatorId: Long,

    @SerializedName("creatorUsername")
    val creatorUsername: String,

    @SerializedName("creatorAvatarUrl")
    val creatorAvatarUrl: String?,
)

// For displaying detailed recipe in recipe detail screen
data class RecipeDetail(
    @SerializedName("id")
    val id: Long,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String?,

    @SerializedName("reminder")
    val reminder: String?,

    @SerializedName("visibility")
    val visibility: RecipeVisibility,

    @SerializedName("likeCount")
    val likeCount: Int,

    @SerializedName("isLikedByCurrentUser")
    val isLikedByCurrentUser: Boolean,

    @SerializedName("images")
    val images: List<RecipeImage>,

    @SerializedName("ingredients")
    val ingredients: List<Ingredient>,

    @SerializedName("steps")
    val steps: List<RecipeStep>,

    @SerializedName("creator")
    val creator: Creator,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updatedAt")
    val updatedAt: String,
)

enum class RecipeVisibility {
    @SerializedName("PUBLIC")
    PUBLIC,

    @SerializedName("PRIVATE")
    PRIVATE,
}

data class RecipeImage(
    @SerializedName("id")
    val id: Long,

    @SerializedName("imageUrl")
    val imageUrl: String,

    @SerializedName("displayOrder")
    val displayOrder: Int,
)

data class Ingredient(
    @SerializedName("id")
    val id: Long,

    @SerializedName("name")
    val name: String,

    @SerializedName("quantity")
    val quantity: String,

    @SerializedName("unit")
    val unit: String?,
)

data class RecipeStep(
    @SerializedName("id")
    val id: Long,

    @SerializedName("stepNumber")
    val stepNumber: Int,

    @SerializedName("description")
    val description: String,

    @SerializedName("imageUrl")
    val imageUrl: String?,
)

data class Creator(
    @SerializedName("id")
    val id: Long,

    @SerializedName("username")
    val username: String,

    @SerializedName("avatarUrl")
    val avatarUrl: String?,
)

// ============== REQUEST MODELS ==============

data class CreateRecipeRequest(
    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("reminder")
    val reminder: String? = null,

    @SerializedName("visibility")
    val visibility: RecipeVisibility = RecipeVisibility.PUBLIC,

    @SerializedName("images")
    val images: List<CreateImageRequest>,

    @SerializedName("ingredients")
    val ingredients: List<CreateIngredientRequest>,

    @SerializedName("steps")
    val steps: List<CreateStepRequest>,
)

data class CreateImageRequest(
    @SerializedName("r2Key")
    val r2Key: String,

    @SerializedName("displayOrder")
    val displayOrder: Int,
)

data class CreateIngredientRequest(
    @SerializedName("name")
    val name: String,

    @SerializedName("quantity")
    val quantity: String,

    @SerializedName("unit")
    val unit: String? = null,
)

data class CreateStepRequest(
    @SerializedName("stepNumber")
    val stepNumber: Int,

    @SerializedName("description")
    val description: String,

    @SerializedName("r2Key")
    val r2Key: String? = null,
)

data class UpdateRecipeRequest(
    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("reminder")
    val reminder: String? = null,

    @SerializedName("visibility")
    val visibility: RecipeVisibility = RecipeVisibility.PUBLIC,

    @SerializedName("images")
    val images: List<CreateImageRequest>,

    @SerializedName("ingredients")
    val ingredients: List<CreateIngredientRequest>,

    @SerializedName("steps")
    val steps: List<CreateStepRequest>,
)

// ============== RESPONSE MODELS ==============

data class LikedResponse(
    @SerializedName("liked")
    val liked: Boolean,
)

// ==================== PAGINATION WRAPPER ====================
data class PageResponse<T>(
    @SerializedName("content")
    val content: List<T>,

    @SerializedName("totalElements")
    val totalElements: Long,

    @SerializedName("totalPages")
    val totalPages: Int,

    @SerializedName("number")
    val number: Int,  // Current page number (0-based)

    @SerializedName("size")
    val size: Int,

    @SerializedName("first")
    val first: Boolean,

    @SerializedName("last")
    val last: Boolean,

    @SerializedName("empty")
    val empty: Boolean
)