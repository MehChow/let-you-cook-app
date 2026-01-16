package com.mehchow.letyoucook

object NavRoutes {
    // ================== CORE ROUTES ==================
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val HOME = "home"
    const val HOME_WITH_USERNAME = "home/{username}"

    // ================== RECIPE ROUTES ==================
    const val RECIPE_DETAIL = "recipe/{recipeId}"
    const val CREATE_RECIPE = "recipe/create"
    const val EDIT_RECIPE = "recipe/edit/{recipeId}"

    // ================== PROFILE ROUTES ==================
    const val PROFILE = "profile"
    const val USER_PROFILE = "user/{userId}"

    // ==================  ROUTE BUILDERS ==================
    fun homeRoute(username: String): String = "$HOME/$username"
    fun recipeDetailRoute(recipeId: Long): String = "recipe/$recipeId"
    fun editRecipeRoute(recipeId: Long): String = "recipe/edit/$recipeId"
    fun userProfileRoute(userId: Long): String = "user/$userId"
}