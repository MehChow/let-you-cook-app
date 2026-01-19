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
import com.mehchow.letyoucook.NavRoutes.homeRoute
import com.mehchow.letyoucook.ui.screens.AuthScreen
import com.mehchow.letyoucook.ui.screens.HomeScreen
import com.mehchow.letyoucook.ui.screens.ProfileScreen
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
        when (val state = authState) {
            is AuthState.Authenticated -> {
                navController.navigate(homeRoute(state.user.username)) {
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
            AuthScreen(onLoginSuccess = { response ->
                navController.navigate(homeRoute(response.username)) {
                    popUpTo(NavRoutes.AUTH) { inclusive = true }
                }
            })
        }

        composable(
            NavRoutes.HOME_WITH_USERNAME,
            arguments = listOf(
                navArgument("username") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: "User"
            HomeScreen(
                username = username,
                onRecipeClick = { recipeId ->
                    navController.navigate(NavRoutes.recipeDetailRoute(recipeId))
                },
                onCreateRecipeClick = {
                    navController.navigate(NavRoutes.CREATE_RECIPE)
                },
                onProfileClick = {
                    navController.navigate(NavRoutes.PROFILE)
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

        // Create recipe screen - placeholder for now
        composable(NavRoutes.CREATE_RECIPE) {
            // TODO: CreateRecipeScreen
            Text("Create Recipe")
        }

        // Profile screen - placeholder for now
        composable(NavRoutes.PROFILE) {
            ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onRecipeClick = { recipeId ->
                    navController.navigate(NavRoutes.recipeDetailRoute(recipeId))
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