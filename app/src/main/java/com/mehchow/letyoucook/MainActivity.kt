package com.mehchow.letyoucook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mehchow.letyoucook.ui.screens.AuthScreen
import com.mehchow.letyoucook.ui.screens.CreateRecipeScreen
import com.mehchow.letyoucook.ui.screens.EditRecipeScreen
import com.mehchow.letyoucook.ui.screens.MainScreen
import com.mehchow.letyoucook.ui.screens.RecipeDetailScreen
import com.mehchow.letyoucook.ui.screens.SplashScreen
import com.mehchow.letyoucook.ui.theme.LetYouCookTheme
import com.mehchow.letyoucook.ui.viewmodel.AuthState
import com.mehchow.letyoucook.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

// telling Hilt to inject dependencies into MainActivity
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Track whether user has manually set a theme preference
            // null = follow system, true = dark, false = light
            var userThemePreference by rememberSaveable { mutableStateOf<Boolean?>(null) }
            
            val systemDarkTheme = isSystemInDarkTheme()
            val isDarkTheme = userThemePreference ?: systemDarkTheme
            
            LetYouCookTheme(darkTheme = isDarkTheme) {
                AppNavigation(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { userThemePreference = !isDarkTheme }
                )
            }
        }
    }
}

@Composable
fun AppNavigation(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val navController = rememberNavController()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                navController.navigate(NavRoutes.MAIN) {
                    popUpTo(NavRoutes.SPLASH) { inclusive = true }
                }
            }

            is AuthState.Unauthenticated -> {
                navController.navigate(NavRoutes.AUTH) {
                    popUpTo(0) { inclusive = true }
                }
            }

            is AuthState.Loading -> {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = NavRoutes.SPLASH
    ) {
        composable(NavRoutes.SPLASH) {
            SplashScreen()
        }

        composable(NavRoutes.AUTH) {
            AuthScreen(onLoginSuccess = {
                navController.navigate(NavRoutes.MAIN) {
                    popUpTo(NavRoutes.AUTH) { inclusive = true }
                }
            })
        }

        // Main screen with bottom navigation (Home, Explore, Notification, Profile)
        composable(NavRoutes.MAIN) { backStackEntry ->
            // Observe the savedStateHandle for recipe changes (created, edited, or deleted)
            val recipeCreated = backStackEntry.savedStateHandle.get<Boolean>("recipe_created") ?: false
            val recipeModified = backStackEntry.savedStateHandle.get<Boolean>("recipe_modified") ?: false
            val shouldRefresh = recipeCreated || recipeModified
            
            MainScreen(
                onRecipeClick = { recipeId ->
                    navController.navigate(NavRoutes.recipeDetailRoute(recipeId))
                },
                onCreateRecipeClick = {
                    navController.navigate(NavRoutes.CREATE_RECIPE)
                },
                onUserProfileClick = { userId ->
                    navController.navigate(NavRoutes.userProfileRoute(userId))
                },
                shouldRefreshHome = shouldRefresh,
                onRefreshConsumed = {
                    // Clear all flags after consuming
                    backStackEntry.savedStateHandle.remove<Boolean>("recipe_created")
                    backStackEntry.savedStateHandle.remove<Boolean>("recipe_modified")
                },
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )
        }

        // Recipe detail screen
        composable(
            route = NavRoutes.RECIPE_DETAIL,
            arguments = listOf(
                navArgument("recipeId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            // Check if recipe was edited and needs refresh
            val recipeEdited = backStackEntry.savedStateHandle.get<Boolean>("recipe_edited") ?: false
            
            RecipeDetailScreen(
                onBackClick = { navController.popBackStack() },
                onCreatorClick = { userId ->
                    navController.navigate(NavRoutes.userProfileRoute(userId))
                },
                onEditClick = { recipeId ->
                    navController.navigate(NavRoutes.editRecipeRoute(recipeId))
                },
                onDeleteSuccess = {
                    // Set flag to refresh home screen and navigate back
                    navController.previousBackStackEntry?.savedStateHandle?.set("recipe_modified", true)
                    navController.popBackStack()
                },
                recipeEdited = recipeEdited,
                onRecipeEdited = {
                    // Clear the flag after consuming
                    backStackEntry.savedStateHandle.remove<Boolean>("recipe_edited")
                }
            )
        }

        // Create recipe screen
        composable(NavRoutes.CREATE_RECIPE) {
            CreateRecipeScreen(
                onBackClick = { navController.popBackStack() },
                onSuccess = {
                    // Set result to trigger refresh on MainScreen
                    navController.previousBackStackEntry?.savedStateHandle?.set("recipe_created", true)
                    navController.popBackStack()
                }
            )
        }
        
        // Edit recipe screen
        composable(
            route = NavRoutes.EDIT_RECIPE,
            arguments = listOf(
                navArgument("recipeId") { type = NavType.LongType }
            )
        ) {
            EditRecipeScreen(
                onBackClick = { navController.popBackStack() },
                onSuccess = {
                    // Set flags to refresh both detail screen and home
                    navController.previousBackStackEntry?.savedStateHandle?.set("recipe_edited", true)
                    // Also notify MainScreen via the detail screen's back stack entry
                    navController.getBackStackEntry(NavRoutes.MAIN).savedStateHandle.set("recipe_modified", true)
                    navController.popBackStack()
                }
            )
        }

        // User profile screen (viewing other users) - placeholder for now
        composable(
            route = NavRoutes.USER_PROFILE,
            arguments = listOf(
                navArgument("userId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong("userId") ?: 0L
            // TODO: UserProfileScreen
            Text("User Profile: $userId")
        }
    }
}