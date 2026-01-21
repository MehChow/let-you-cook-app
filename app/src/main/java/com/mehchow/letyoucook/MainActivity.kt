package com.mehchow.letyoucook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.mehchow.letyoucook.ui.screens.EditProfileScreen
import com.mehchow.letyoucook.ui.screens.EditRecipeScreen
import com.mehchow.letyoucook.ui.screens.MainScreen
import com.mehchow.letyoucook.ui.screens.RecipeCreatedScreen
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
            // Use ViewModel to get persisted theme preference
            val viewModel: MainViewModel = hiltViewModel()
            val themePreference by viewModel.isDarkTheme.collectAsState()
            
            val systemDarkTheme = isSystemInDarkTheme()
            // null = follow system, otherwise use the saved preference
            val isDarkTheme = themePreference ?: systemDarkTheme
            
            // Update system bars when theme changes
            LaunchedEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        // Dark theme: light text on dark background
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        // Light theme: dark text on light background
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }
            
            LetYouCookTheme(darkTheme = isDarkTheme) {
                AppNavigation(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { viewModel.toggleTheme(isDarkTheme) }
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
            
            // Observe for profile updates (separate flag for home refresh)
            val profileUpdated = backStackEntry.savedStateHandle.get<Boolean>("profile_updated") ?: false
            val profileUpdatedHome = backStackEntry.savedStateHandle.get<Boolean>("profile_updated_home") ?: false
            
            // Home should refresh on recipe changes OR profile updates (to update user info on recipe cards)
            val shouldRefreshHome = recipeCreated || recipeModified || profileUpdatedHome
            
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
                onEditProfileClick = {
                    navController.navigate(NavRoutes.EDIT_PROFILE)
                },
                shouldRefreshHome = shouldRefreshHome,
                onRefreshConsumed = {
                    // Clear all home-related flags after consuming
                    backStackEntry.savedStateHandle.remove<Boolean>("recipe_created")
                    backStackEntry.savedStateHandle.remove<Boolean>("recipe_modified")
                    backStackEntry.savedStateHandle.remove<Boolean>("profile_updated_home")
                },
                shouldRefreshProfile = profileUpdated,
                onProfileRefreshConsumed = {
                    backStackEntry.savedStateHandle.remove<Boolean>("profile_updated")
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
                    // Set flag to trigger refresh on MainScreen when we get back there
                    navController.getBackStackEntry(NavRoutes.MAIN).savedStateHandle.set("recipe_created", true)
                    // Navigate to success screen, removing CREATE_RECIPE from back stack
                    navController.navigate(NavRoutes.RECIPE_CREATED) {
                        popUpTo(NavRoutes.CREATE_RECIPE) { inclusive = true }
                    }
                }
            )
        }

        // Recipe created success screen (full screen with animation)
        composable(NavRoutes.RECIPE_CREATED) {
            RecipeCreatedScreen(
                onNavigateToHome = {
                    // Pop back to MAIN (the flag is already set)
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

        // Edit profile screen
        composable(NavRoutes.EDIT_PROFILE) {
            EditProfileScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = {
                    // Set flags to refresh both profile and home when we go back
                    // profile_updated - refreshes profile tab
                    // profile_updated_home - refreshes home tab (to update user info on recipe cards)
                    navController.previousBackStackEntry?.savedStateHandle?.set("profile_updated", true)
                    navController.previousBackStackEntry?.savedStateHandle?.set("profile_updated_home", true)
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