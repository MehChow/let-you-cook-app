package com.mehchow.letyoucook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mehchow.letyoucook.ui.screens.AuthScreen
import com.mehchow.letyoucook.ui.screens.CreateRecipeScreen
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
            LetYouCookTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(
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
            // Observe the savedStateHandle for recipe_created result
            val recipeCreated = backStackEntry.savedStateHandle.get<Boolean>("recipe_created") ?: false
            
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
                shouldRefreshHome = recipeCreated,
                onRefreshConsumed = {
                    // Clear the flag after consuming
                    backStackEntry.savedStateHandle.remove<Boolean>("recipe_created")
                }
            )
        }

        // Recipe detail screen
        composable(
            route = NavRoutes.RECIPE_DETAIL,
            arguments = listOf(
                navArgument("recipeId") { type = NavType.LongType }
            )
        ) {
            RecipeDetailScreen(
                onBackClick = { navController.popBackStack() },
                onCreatorClick = { userId ->
                    navController.navigate(NavRoutes.userProfileRoute(userId))
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