package com.mehchow.letyoucook

object NavRoutes {
    // ================== CORE ROUTES ==================
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val MAIN = "main"  // Main screen with bottom navigation

    // ================== BOTTOM NAV TAB ROUTES ==================
    const val TAB_HOME = "tab_home"
    const val TAB_EXPLORE = "tab_explore"
    const val TAB_NOTIFICATION = "tab_notification"
    const val TAB_PROFILE = "tab_profile"

    // ================== RECIPE ROUTES ==================
    const val RECIPE_DETAIL = "recipe/{recipeId}"
    const val CREATE_RECIPE = "recipe/create"
    const val EDIT_RECIPE = "recipe/edit/{recipeId}"
    const val RECIPE_CREATED = "recipe/created"  // Success screen after creation

    // ================== USER ROUTES ==================
    const val USER_PROFILE = "user/{userId}"

    // ==================  ROUTE BUILDERS ==================
    fun recipeDetailRoute(recipeId: Long): String = "recipe/$recipeId"
    fun editRecipeRoute(recipeId: Long): String = "recipe/edit/$recipeId"
    fun userProfileRoute(userId: Long): String = "user/$userId"
}